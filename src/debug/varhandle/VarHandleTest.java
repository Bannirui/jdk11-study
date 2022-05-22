package debug.varhandle;

import javax.xml.stream.events.StartDocument;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * <h4>变量句柄<h4/>
 *
 * <p>线程同步手段方式
 * <ul>
 * <li>加锁 线程上下文切换开销</li>
 * <li>Atomic原子类 开销和aba问题</li>
 * <li>原子性FieldUpdaters反射 反射开销</li>
 * <li>Unsafe 损害安全性和可移植性</li>
 * </ul>
 *
 * <p>针对上述4个解决线程同步的方式都有一定的弊端问题 {@link java.lang.invoke.VarHandle} 就是用来替代的一种解方案
 *
 * <p>优势
 * <ul>
 *     <li>提供了一系列标准的内存屏障操作</li>
 *     <li>用于更细粒度的控制指令排序</li>
 *     <li>在安全性 可用性 性能方面都要优于现有的AIP</li>
 *     <li>基本上能够和任何类型的变量相关联</li>
 * </ul>
 *
 * @since 2022/2/17
 * @author Dingrui
 */
public class VarHandleTest {

    private String planStr; // 普通变量
    private static String staticStr; // 静态变量
    private String reflectStr; // 反射生成变量
    private String[] arrStr = new String[10]; // 数组变量

    public VarHandleTest() {
    }

    public String getPlanStr() {
        return planStr;
    }

    public void setPlanStr(String planStr) {
        this.planStr = planStr;
    }

    public static String getStaticStr() {
        return staticStr;
    }

    public static void setStaticStr(String staticStr) {
        VarHandleTest.staticStr = staticStr;
    }

    public String getReflectStr() {
        return reflectStr;
    }

    public void setReflectStr(String reflectStr) {
        this.reflectStr = reflectStr;
    }

    public String[] getArrStr() {
        return arrStr;
    }

    public void setArrStr(String[] arrStr) {
        this.arrStr = arrStr;
    }

    public void setPlainVarRelaxed(String s) {
        plainVar.set(this, s);
    }

    private static VarHandle plainVar;
    private static VarHandle staticVar;
    private static VarHandle reflectVar;
    private static VarHandle arrVar;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            plainVar = l.findVarHandle(VarHandleTest.class, "planStr", String.class);
            staticVar = l.findStaticVarHandle(VarHandleTest.class, "staticStr", String.class);
            reflectVar = l.unreflectVarHandle(VarHandleTest.class.getDeclaredField("reflectStr"));
            arrVar = MethodHandles.arrayElementVarHandle(String[].class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        VarHandleTest obj = new VarHandleTest();
        obj.setPlanStr("hello");
        obj.setPlainVarRelaxed("dingrui");
        System.out.println();
    }
}
