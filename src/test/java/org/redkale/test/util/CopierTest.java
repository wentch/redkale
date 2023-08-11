/*
 *
 */
package org.redkale.test.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.*;
import org.redkale.convert.json.JsonConvert;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class CopierTest {

    public static void main(String[] args) throws Throwable {
        CopierTest test = new CopierTest();
        test.run1();
        test.run2();
        test.run3();
        test.run4();
        test.run5();
        test.run6();
        test.run7();
        test.run8();
        test.run9();
        test.run10();
        test.run11();
        test.run12();
        test.run13();
        test.run14();
        test.run15();
        test.run16();
        test.run17();
        test.run18();
        test.run19();
    }

    @Test
    public void run1() throws Exception {
        TestBean bean = new TestBean();
        bean.setId(222);
        bean.time = 55555L;
        bean.setName("haha");
        bean.setMap(Utility.ofMap("aa", "bbb"));
        Map map = new TreeMap(Copier.copy(bean, Map.class));
        System.out.println(JsonConvert.root().convertTo(map));
        TreeMap rs = Copier.copy(bean, TreeMap.class);
        rs.remove("remark");
        rs.remove("seqno");
        Assertions.assertEquals(bean.toString(), JsonConvert.root().convertTo(rs));
    }

    @Test
    public void run2() throws Exception {
        TestBean bean = new TestBean();
        bean.setId(222);
        bean.time = 55555L;
        bean.setName("haha");
        bean.setMap(Utility.ofMap("aa", "bbb"));
        TreeMap rs = new TreeMap();
        Copier.load(TestInterface.class, Map.class).apply(bean, rs);
        System.out.println(JsonConvert.root().convertTo(rs));
    }

    @Test
    public void run3() throws Exception {
        Map map = new LinkedHashMap();
        map.put("name", "haha");
        map.put("time", "55555");
        map.put("id", "222");
        map.put("map", Utility.ofMap("aa", "bbb"));
        TestBean bean = new TestBean();
        TestInterface ti = Copier.load(Map.class, TestInterface.class).apply(map, new TestBean());;
        Assertions.assertEquals("{\"id\":222,\"map\":{\"aa\":\"bbb\"},\"time\":0}", JsonConvert.root().convertTo(ti));
    }

    @Test
    public void run4() throws Exception {
        TestBean bean = new TestBean();
        Map map = new TreeMap();
        map.put("name", "haha");
        map.put("time", "55555");
        map.put("id", "222");
        map.put("map", Utility.ofMap("aa", "bbb"));
        Copier.load(Map.class, TestBean.class).apply(map, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        map.put("time", 55555L);
        map.put("id", 222);
        Assertions.assertEquals(bean.toString(), JsonConvert.root().convertTo(map));
    }

    @Test
    public void run5() throws Exception {
        Map map = new TreeMap();
        map.put("name", "haha");
        map.put("time", "55555");
        map.put("id", "222");
        map.put("map", Utility.ofMap("aa", "bbb"));
        Map rs = new TreeMap();
        Copier.load(Map.class, Map.class).apply(map, rs);
        System.out.println("Map: " + JsonConvert.root().convertTo(rs));
        Assertions.assertEquals(JsonConvert.root().convertTo(map), JsonConvert.root().convertTo(rs));
    }

    @Test
    public void run6() throws Exception {
        TestBean bean = new TestBean();
        bean.setId(222);
        bean.time = 55555L;
        bean.setName(null);
        bean.setMap(Utility.ofMap("aa", "bbb"));
        ConcurrentHashMap rs = Copier.copy(bean, ConcurrentHashMap.class);
        System.out.println(JsonConvert.root().convertTo(rs));
        System.out.println("------------------------------------------");
    }

    @Test
    public void run7() throws Exception {
        TestBean bean = new TestBean();
        Map map = new TreeMap();
        map.put("name", "haha");
        map.put("time", "55555");
        map.put("id", null);
        map.put("map", Utility.ofMap("aa", "bbb"));
        Copier.load(Map.class, TestBean.class).apply(map, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        System.out.println("------------------------------------------");
    }

    @Test
    public void run8() throws Exception {
        TestBean bean = new TestBean();
        Map map = new TreeMap();
        map.put("name", "");
        map.put("time", "55555");
        map.put("id", null);
        map.put("map", Utility.ofMap("aa", "bbb"));
        Copier.load(Map.class, TestBean.class, Copier.OPTION_SKIP_RMPTY_STRING).apply(map, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        Assertions.assertTrue(bean.getName() == null);
    }

    @Test
    public void run9() throws Exception {
        TestBean bean = new TestBean();
        bean.remark = "hehehoho";
        Map map = new TreeMap();
        map.put("name", "");
        map.put("time", "55555");
        map.put("id", null);
        map.put("remark", null);
        map.put("map", Utility.ofMap("aa", "bbb"));
        Copier.load(Map.class, TestBean.class).apply(map, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        Assertions.assertTrue(bean.remark == null);

        bean.remark = "hehehoho";
        Copier.load(Map.class, TestBean.class, Copier.OPTION_SKIP_NULL_VALUE).apply(map, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        Assertions.assertTrue(bean.remark != null);
    }

    @Test
    public void run10() throws Exception {
        TestBean bean = new TestBean();
        bean.remark = "hehehoho";
        TestXBean srcBean = new TestXBean();
        srcBean.setName("");
        srcBean.time = 55555;
        srcBean.remark = null;
        srcBean.setMap(Utility.ofMap("aa", "bbb"));
        Copier.load(TestXBean.class, TestBean.class).apply(srcBean, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        Assertions.assertTrue(bean.remark == null);

        bean.remark = "hehehoho";
        Copier.load(TestXBean.class, TestBean.class, Copier.OPTION_SKIP_NULL_VALUE).apply(srcBean, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        Assertions.assertTrue(bean.remark != null);
    }

    @Test
    public void run11() throws Exception {
        TestBean bean = new TestBean();
        TestXBean srcBean = new TestXBean();
        srcBean.setName("");
        srcBean.time = 55555;
        srcBean.remark = null;
        srcBean.setMap(Utility.ofMap("aa", "bbb"));
        Copier.load(TestXBean.class, TestBean.class, Copier.OPTION_SKIP_RMPTY_STRING).apply(srcBean, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        Assertions.assertTrue(bean.getName() == null);
    }

    @Test
    public void run12() throws Exception {
        TestBean bean = new TestBean();
        bean.remark = "hehehoho";
        TestXBean srcBean = new TestXBean();
        srcBean.setName("");
        srcBean.time = 55555;
        srcBean.remark = null;
        srcBean.setMap(Utility.ofMap("aa", "bbb"));
        Copier.load(TestXBean.class, TestBean.class).apply(srcBean, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        Assertions.assertTrue(bean.remark == null);

        bean.setName(null);
        bean.remark = "hehehoho";
        Copier.load(TestXBean.class, TestBean.class, Copier.OPTION_SKIP_NULL_VALUE | Copier.OPTION_SKIP_RMPTY_STRING).apply(srcBean, bean);
        System.out.println(JsonConvert.root().convertTo(bean));
        Assertions.assertTrue(bean.getName() == null);
    }

    @Test
    public void run13() throws Exception {
        TestBean bean = new TestBean();
        bean.setSeqno(6666L);
        //public String remark;    
        //private Long seqno;
        TestX2Bean destBean = new TestX2Bean();
        //public int remark;
        //private String seqno;

        Copier.load(TestBean.class, TestX2Bean.class, Copier.OPTION_ALLOW_TYPE_CAST).apply(bean, destBean);
        System.out.println(JsonConvert.root().convertTo(destBean));
        Assertions.assertEquals("6666", destBean.getSeqno());
    }

    @Test
    public void run14() throws Exception {
        TestBean bean = new TestBean();
        bean.remark = "444";
        TestX2Bean destBean = new TestX2Bean();
        Copier.load(TestBean.class, TestX2Bean.class, Copier.OPTION_SKIP_NULL_VALUE | Copier.OPTION_ALLOW_TYPE_CAST).apply(bean, destBean);
        System.out.println(JsonConvert.root().convertTo(destBean));
        Assertions.assertTrue(destBean.remark == 444);
    }

    @Test
    public void run15() throws Exception {
        Bean1 bean1 = new Bean1();
        bean1.intval = "444";
        Bean2 bean2 = new Bean2();
        Copier.load(Bean1.class, Bean2.class, Copier.OPTION_SKIP_NULL_VALUE | Copier.OPTION_ALLOW_TYPE_CAST).apply(bean1, bean2);
        System.out.println(JsonConvert.root().convertTo(bean2));
        Assertions.assertTrue(bean2.intval == 444);
    }

    @Test
    public void run16() throws Exception {
        Bean3 bean1 = new Bean3();
        bean1.setSeqno(444L);
        Bean4 bean2 = new Bean4();
        Copier.load(Bean3.class, Bean4.class, Copier.OPTION_SKIP_NULL_VALUE | Copier.OPTION_ALLOW_TYPE_CAST).apply(bean1, bean2);
        System.out.println(JsonConvert.root().convertTo(bean2));
        Assertions.assertEquals("444", bean2.getSeqno());
    }

    @Test
    public void run17() throws Exception {
        Bean4 bean1 = new Bean4();
        bean1.setSeqno("444");
        Bean5 bean2 = new Bean5();
        Copier.load(Bean4.class, Bean5.class, Copier.OPTION_SKIP_NULL_VALUE | Copier.OPTION_ALLOW_TYPE_CAST).apply(bean1, bean2);
        System.out.println(JsonConvert.root().convertTo(bean2));
        Assertions.assertEquals(444, bean2.getSeqno());
    }

    @Test
    public void run18() throws Exception {
        Bean4 bean1 = new Bean4();
        bean1.setSeqno("444");
        Bean5 bean2 = new Bean5();
        Copier.load(Bean4.class, Bean5.class, Copier.OPTION_SKIP_NULL_VALUE).apply(bean1, bean2);
        System.out.println(JsonConvert.root().convertTo(bean2));
        Assertions.assertEquals(0, bean2.getSeqno());
    }

    @Test
    public void run19() throws Exception {
        Bean0 bean1 = new Bean0();
        bean1.setCartype(111); 
        bean1.setUsername("aaa");
        Bean0 bean2 = new Bean0();
        Copier.load(Bean0.class, Bean0.class, Copier.OPTION_SKIP_NULL_VALUE).apply(bean1, bean2);
        System.out.println(JsonConvert.root().convertTo(bean2));
        Assertions.assertEquals("aaa", bean2.getUsername());
        System.out.println("------------------------------------------");
    }

    public class Bean0 {

        private long carid;

        private int cartype;

        private int userid;

        private String username;

        private String cartitle;

        public Bean0() {
        }

        public long getCarid() {
            return carid;
        }

        public void setCarid(long carid) {
            this.carid = carid;
        }

        public int getUserid() {
            return userid;
        }

        public void setUserid(int userid) {
            this.userid = userid;
        }

        public String getCartitle() {
            return cartitle;
        }

        public void setCartitle(String cartitle) {
            this.cartitle = cartitle;
        }

        public int getCartype() {
            return cartype;
        }

        public void setCartype(int cartype) {
            this.cartype = cartype;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }

    }

    public class Bean1 {

        public String intval;

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }
    }

    public class Bean2 {

        public int intval;

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }
    }

    public class Bean3 {

        private Long seqno;

        public Long getSeqno() {
            return seqno;
        }

        public void setSeqno(Long seqno) {
            this.seqno = seqno;
        }

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }
    }

    public class Bean4 {

        private String seqno;

        public String getSeqno() {
            return seqno;
        }

        public void setSeqno(String seqno) {
            this.seqno = seqno;
        }

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }
    }

    public class Bean5 {

        private int seqno;

        public int getSeqno() {
            return seqno;
        }

        public void setSeqno(int seqno) {
            this.seqno = seqno;
        }

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }
    }
}
