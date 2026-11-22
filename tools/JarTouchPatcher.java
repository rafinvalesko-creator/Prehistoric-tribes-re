import org.objectweb.asm.*;
import java.io.*;
import java.nio.file.*;
import java.util.jar.*;

public final class JarTouchPatcher {
  static final String TARGET = "g";
  static final String HELPER = "TouchKeyRelease";

  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new IllegalArgumentException("input.jar output.jar");
    Path input = Paths.get(args[0]), output = Paths.get(args[1]);
    boolean[] patched = {false};
    int[] hiddenCursorDraws = {0};
    try (JarFile jar = new JarFile(input.toFile());
         JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
      java.util.Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        if (name.matches("(?i)META-INF/[^/]+\\.(SF|RSA|DSA)$")) continue;
        JarEntry copy = new JarEntry(name);
        out.putNextEntry(copy);
        try (InputStream in = jar.getInputStream(entry)) {
          if (name.equals(TARGET + ".class")) {
            byte[] original = readAll(in);
            out.write(patchGameClass(original, hiddenCursorDraws));
            patched[0] = true;
          } else {
            in.transferTo(out);
          }
        }
        out.closeEntry();
      }
      if (!patched[0]) throw new IllegalStateException("Class g.class not found in original JAR");
      out.putNextEntry(new JarEntry(HELPER + ".class"));
      out.write(makeReleaseClass());
      out.closeEntry();
    }
    if (hiddenCursorDraws[0] < 2) {
      throw new IllegalStateException("Expected both cursor draw branches to be removed, removed " + hiddenCursorDraws[0]);
    }
    System.out.println("Touch input patch applied: removed " + hiddenCursorDraws[0] + " battlefield cursor draw calls");
  }

  static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    in.transferTo(b);
    return b.toByteArray();
  }

  static byte[] patchGameClass(byte[] source, int[] hiddenCursorDraws) {
    ClassReader reader = new ClassReader(source);
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
      @Override protected String getCommonSuperClass(String a, String b) { return "java/lang/Object"; }
    };
    ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
      boolean target;
      boolean hasPress, hasRelease, hasDispatch, hasX, hasY, hasTime;
      @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        target = TARGET.equals(name);
        if (!target) throw new IllegalStateException("Expected game class g, got " + name);
        super.visit(version, access, name, signature, superName, interfaces);
      }
      @Override public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
        if (name.equals("touchStartX")) hasX = true;
        if (name.equals("touchStartY")) hasY = true;
        if (name.equals("touchStartTime")) hasTime = true;
        return super.visitField(access, name, desc, sig, value);
      }
      @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
        if (name.equals("pointerPressed") && desc.equals("(II)V")) hasPress = true;
        if (name.equals("pointerReleased") && desc.equals("(II)V")) hasRelease = true;
        if (name.equals("touchDispatch") && desc.equals("(I)V")) hasDispatch = true;
        MethodVisitor downstream = super.visitMethod(access, name, desc, sig, exceptions);
        // Hide only the original battlefield hand sprite, leaving other UI icons intact.
        if (name.equals("d") && desc.equals("(II)V")) {
          return new MethodVisitor(Opcodes.ASM9, downstream) {
            @Override public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean isInterface) {
              // Both alternate draw branches render the same hand sprite; hide every call.
              if (owner.equals(TARGET) && method.equals("d") && descriptor.equals("(IIII)V")) {
                hiddenCursorDraws[0]++;
                for (int i = 0; i < 5; i++) super.visitInsn(Opcodes.POP);
                return;
              }
              super.visitMethodInsn(opcode, owner, method, descriptor, isInterface);
            }
          };
        }
        return downstream;
      }
      @Override public void visitEnd() {
        if (!hasX) super.visitField(Opcodes.ACC_PRIVATE, "touchStartX", "I", null, null).visitEnd();
        if (!hasY) super.visitField(Opcodes.ACC_PRIVATE, "touchStartY", "I", null, null).visitEnd();
        if (!hasTime) super.visitField(Opcodes.ACC_PRIVATE, "touchStartTime", "J", null, null).visitEnd();
        if (!hasPress) addPointerPressed(cv());
        if (!hasRelease) addPointerReleased(cv());
        if (!hasDispatch) addTouchDispatch(cv());
        super.visitEnd();
      }
      ClassVisitor cv() { return this; }
      void addPointerPressed(ClassVisitor c) {
        MethodVisitor m = c.visitMethod(Opcodes.ACC_PROTECTED, "pointerPressed", "(II)V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitFieldInsn(Opcodes.PUTFIELD, TARGET, "touchStartX", "I");
        m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.ILOAD, 2);
        m.visitFieldInsn(Opcodes.PUTFIELD, TARGET, "touchStartY", "I");
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        m.visitFieldInsn(Opcodes.PUTFIELD, TARGET, "touchStartTime", "J");
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0,0); m.visitEnd();
      }
      void addPointerReleased(ClassVisitor c) {
        MethodVisitor m = c.visitMethod(Opcodes.ACC_PROTECTED, "pointerReleased", "(II)V", null, null);
        Label swipe = new Label(), vertical = new Label(), right = new Label(), down = new Label(), dispatch = new Label(), done = new Label();
        m.visitCode();
        m.visitVarInsn(Opcodes.ILOAD, 1); m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"touchStartX","I"); m.visitInsn(Opcodes.ISUB);
        m.visitVarInsn(Opcodes.ISTORE,3);
        m.visitVarInsn(Opcodes.ILOAD, 2); m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"touchStartY","I"); m.visitInsn(Opcodes.ISUB);
        m.visitVarInsn(Opcodes.ISTORE,4);
        m.visitVarInsn(Opcodes.ILOAD,3); m.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/Math","abs","(I)I",false);
        m.visitVarInsn(Opcodes.ILOAD,4); m.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/Math","abs","(I)I",false);
        m.visitInsn(Opcodes.IADD); m.visitIntInsn(Opcodes.BIPUSH,18); m.visitJumpInsn(Opcodes.IF_ICMPGT,swipe);
        // A stationary long press opens the original game's context/action menu.
        Label shortTap = new Label();
        m.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/System","currentTimeMillis","()J",false);
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"touchStartTime","J");
        m.visitInsn(Opcodes.LSUB);
        m.visitLdcInsn(450L);
        m.visitInsn(Opcodes.LCMP);
        m.visitJumpInsn(Opcodes.IFLE,shortTap);
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitIntInsn(Opcodes.BIPUSH,42);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TARGET,"keyPressed","(I)V",false);
        m.visitJumpInsn(Opcodes.GOTO,done);
        m.visitLabel(shortTap);
        // In the battlefield state, convert the tap position into the game's 22x16 map grid.
        Label notMap = new Label(), mapTap = new Label(), menuTap = new Label(), menuIndexOk = new Label();
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"v","B");
        m.visitInsn(Opcodes.ICONST_1);
        m.visitJumpInsn(Opcodes.IF_ICMPNE,notMap);
        // Command wheel (game state w=2) is laid out horizontally.
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"w","B");
        m.visitInsn(Opcodes.ICONST_2);
        m.visitJumpInsn(Opcodes.IF_ICMPNE,mapTap);
        m.visitFieldInsn(Opcodes.GETSTATIC,TARGET,"aU","I");
        m.visitJumpInsn(Opcodes.IFEQ,mapTap);
        m.visitVarInsn(Opcodes.ILOAD,1);
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"aX","I");
        m.visitInsn(Opcodes.ISUB);
        m.visitFieldInsn(Opcodes.GETSTATIC,TARGET,"aU","I");
        m.visitInsn(Opcodes.IDIV);
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"aW","I");
        m.visitInsn(Opcodes.IADD);
        m.visitVarInsn(Opcodes.ISTORE,5);
        m.visitVarInsn(Opcodes.ILOAD,5);
        m.visitJumpInsn(Opcodes.IFGE,menuIndexOk);
        m.visitInsn(Opcodes.ICONST_0);
        m.visitVarInsn(Opcodes.ISTORE,5);
        m.visitLabel(menuIndexOk);
        m.visitVarInsn(Opcodes.ILOAD,5);
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"ap","B");
        m.visitJumpInsn(Opcodes.IF_ICMPLT,menuTap);
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"ap","B");
        m.visitInsn(Opcodes.ICONST_1);
        m.visitInsn(Opcodes.ISUB);
        m.visitVarInsn(Opcodes.ISTORE,5);
        m.visitLabel(menuTap);
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitVarInsn(Opcodes.ILOAD,5);
        m.visitInsn(Opcodes.I2B);
        m.visitFieldInsn(Opcodes.PUTFIELD,TARGET,"aq","B");
        m.visitJumpInsn(Opcodes.GOTO,notMap);
        m.visitLabel(mapTap);
        // Invert the game's original cursor-to-screen transform for map tiles.
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitVarInsn(Opcodes.ILOAD,1);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"aH","I");
        m.visitInsn(Opcodes.ISUB); m.visitIntInsn(Opcodes.BIPUSH,11); m.visitInsn(Opcodes.IADD);
        m.visitInsn(Opcodes.ICONST_5); m.visitInsn(Opcodes.IDIV);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"W","I"); m.visitInsn(Opcodes.IADD);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"I","B"); m.visitInsn(Opcodes.ISUB);
        m.visitInsn(Opcodes.I2B); m.visitFieldInsn(Opcodes.PUTFIELD,TARGET,"y","B");
        m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitVarInsn(Opcodes.ILOAD,2);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"aI","I");
        m.visitInsn(Opcodes.ISUB); m.visitIntInsn(Opcodes.BIPUSH,8); m.visitInsn(Opcodes.IADD);
        m.visitInsn(Opcodes.ICONST_4); m.visitInsn(Opcodes.IDIV);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"X","I"); m.visitInsn(Opcodes.IADD);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"aJ","I"); m.visitInsn(Opcodes.ISUB);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,TARGET,"K","B"); m.visitInsn(Opcodes.ISUB);
        m.visitInsn(Opcodes.I2B); m.visitFieldInsn(Opcodes.PUTFIELD,TARGET,"A","B");
        m.visitLabel(notMap);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitIntInsn(Opcodes.BIPUSH,53);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TARGET,"keyPressed","(I)V",false);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitIntInsn(Opcodes.BIPUSH,53);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TARGET,"keyReleased","(I)V",false);
        m.visitJumpInsn(Opcodes.GOTO,done);
        m.visitLabel(swipe);
        m.visitVarInsn(Opcodes.ILOAD,3); m.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/Math","abs","(I)I",false);
        m.visitVarInsn(Opcodes.ILOAD,4); m.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/Math","abs","(I)I",false);
        m.visitJumpInsn(Opcodes.IF_ICMPLE,vertical);
        m.visitVarInsn(Opcodes.ILOAD,3); m.visitJumpInsn(Opcodes.IFGE,right);
        m.visitIntInsn(Opcodes.BIPUSH,52); m.visitJumpInsn(Opcodes.GOTO,dispatch);
        m.visitLabel(right); m.visitIntInsn(Opcodes.BIPUSH,54); m.visitJumpInsn(Opcodes.GOTO,dispatch);
        m.visitLabel(vertical);
        m.visitVarInsn(Opcodes.ILOAD,4); m.visitJumpInsn(Opcodes.IFGE,down);
        m.visitIntInsn(Opcodes.BIPUSH,50); m.visitJumpInsn(Opcodes.GOTO,dispatch);
        m.visitLabel(down); m.visitIntInsn(Opcodes.BIPUSH,56);
        m.visitLabel(dispatch);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitInsn(Opcodes.SWAP);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL,TARGET,"touchDispatch","(I)V",false);
        m.visitLabel(done);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0,0); m.visitEnd();
      }
      void addTouchDispatch(ClassVisitor c) {
        MethodVisitor m = c.visitMethod(Opcodes.ACC_PRIVATE, "touchDispatch", "(I)V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitVarInsn(Opcodes.ILOAD,1);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TARGET,"keyPressed","(I)V",false);
        m.visitTypeInsn(Opcodes.NEW,"java/lang/Thread"); m.visitInsn(Opcodes.DUP);
        m.visitTypeInsn(Opcodes.NEW,HELPER); m.visitInsn(Opcodes.DUP);
        m.visitVarInsn(Opcodes.ALOAD,0); m.visitVarInsn(Opcodes.ILOAD,1);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL,HELPER,"<init>","(Lg;I)V",false);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Thread","<init>","(Ljava/lang/Runnable;)V",false);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/Thread","start","()V",false);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0,0); m.visitEnd();
      }
    };
    reader.accept(visitor, ClassReader.EXPAND_FRAMES);
    return writer.toByteArray();
  }

  static byte[] makeReleaseClass() {
    ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, HELPER, null, "java/lang/Object", new String[]{"java/lang/Runnable"});
    w.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "target", "Lg;", null, null).visitEnd();
    w.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "key", "I", null, null).visitEnd();
    MethodVisitor m = w.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Lg;I)V", null, null);
    m.visitCode(); m.visitVarInsn(Opcodes.ALOAD,0); m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);
    m.visitVarInsn(Opcodes.ALOAD,0); m.visitVarInsn(Opcodes.ALOAD,1); m.visitFieldInsn(Opcodes.PUTFIELD,HELPER,"target","Lg;");
    m.visitVarInsn(Opcodes.ALOAD,0); m.visitVarInsn(Opcodes.ILOAD,2); m.visitFieldInsn(Opcodes.PUTFIELD,HELPER,"key","I");
    m.visitInsn(Opcodes.RETURN); m.visitMaxs(0,0); m.visitEnd();
    m = w.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
    Label start = new Label(), end = new Label(), handler = new Label(), done = new Label();
    m.visitTryCatchBlock(start,end,handler,"java/lang/InterruptedException");
    m.visitCode(); m.visitLabel(start); m.visitLdcInsn(90L);
    m.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/Thread","sleep","(J)V",false);
    m.visitLabel(end); m.visitJumpInsn(Opcodes.GOTO,done);
    m.visitLabel(handler); m.visitVarInsn(Opcodes.ASTORE,1);
    m.visitLabel(done); m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,HELPER,"target","Lg;");
    m.visitVarInsn(Opcodes.ALOAD,0); m.visitFieldInsn(Opcodes.GETFIELD,HELPER,"key","I");
    m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"g","keyReleased","(I)V",false);
    m.visitInsn(Opcodes.RETURN); m.visitMaxs(0,0); m.visitEnd();
    w.visitEnd(); return w.toByteArray();
  }
}