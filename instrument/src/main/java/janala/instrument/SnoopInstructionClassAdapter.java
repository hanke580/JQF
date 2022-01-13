
package janala.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Map;

public class SnoopInstructionClassAdapter extends ClassVisitor {
  private final String className;
  private List<Map.Entry<String, String>> funcToInst;
  private String superName;

  public SnoopInstructionClassAdapter(ClassVisitor cv, String className, List<Map.Entry<String, String>> funcToInst) {
    super(Opcodes.ASM8, cv);
    this.className = className;
    this.funcToInst = funcToInst;
  }

  @Override
  public void visit(int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces) {
    assert name.equals(this.className);
    this.superName = superName;
    cv.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, 
      String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

    // Check the function to instrument
    boolean toInstrument = false;
    for (Map.Entry<String, String> e : funcToInst) {
      if (e.getKey().equals(name) && e.getValue().equals(desc)) {
        toInstrument = true;
        break;
      }
    }
    if (!toInstrument) return mv;
    System.out.println(String.format("Instrumenting: %s%s", name, desc));

    if (mv != null) {
      return new SnoopInstructionMethodAdapter(mv, className, name, desc, superName,
          GlobalStateForInstrumentation.instance);
    }
    return null;
  }
}
