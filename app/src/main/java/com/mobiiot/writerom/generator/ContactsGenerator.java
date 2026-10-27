package com.mobiiot.writerom.generator;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 电话联系人发生器：
 * 用于批量高速向系统 ContactsProvider 注入真实结构化联系人数据，
 * 压测 SQLite 数据库膨胀、拨号盘 T9 搜索、拼音索引及 CursorWindow 性能。
 *
 * 特性：
 * 1. 扩充海量姓氏 (220+) 与名字库 (14,480+)，组合空间达 318 万+ 真实自然中文姓名；
 * 2. 数学同余双射置换 + 运行时数据库碰撞检测，严密杜绝任何重复姓名生成；
 * 3. 涵盖移动、联通、电信、广电及虚拟运营商 44 种号段，电话号码真实多样；
 * 4. 独立专属账户 (com.mobiiot.writerom) 隔离，一键原子物理硬删除，绝不污染真实通讯录。
 */
public class ContactsGenerator {

    public static final String ACCOUNT_TYPE = "com.mobiiot.writerom";
    public static final String ACCOUNT_NAME = "WriteRom_Stress";
    public static final String NAME_PREFIX = "[WriteRom] ";

    // 已存在姓名缓存（用于进程生命周期与多线程跨批次防重校验）
    private static final Set<String> sExistingNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static volatile boolean sExistingNamesLoaded = false;
    private static final Object sLoadLock = new Object();
    private static final AtomicLong sGlobalNameSeq = new AtomicLong(0);

    // 常用中文姓氏库（常见单姓 200 个 + 常见复姓 20 个 = 220 个）
    private static final String[] SURNAMES = {
            "李", "王", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
            "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗",
            "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧",
            "程", "曹", "袁", "邓", "许", "傅", "沈", "曾", "彭", "吕",
            "苏", "卢", "蒋", "蔡", "贾", "丁", "魏", "薛", "叶", "阎",
            "余", "潘", "杜", "戴", "夏", "钟", "汪", "田", "任", "姜",
            "范", "方", "石", "姚", "谭", "廖", "邹", "熊", "金", "陆",
            "郝", "孔", "白", "崔", "康", "毛", "邱", "秦", "江", "史",
            "顾", "侯", "邵", "孟", "龙", "万", "段", "漕", "钱", "汤",
            "尹", "黎", "易", "常", "武", "乔", "贺", "赖", "龚", "文",
            "庞", "樊", "兰", "殷", "施", "陶", "洪", "翟", "安", "颜",
            "倪", "严", "牛", "温", "芦", "季", "俞", "章", "鲁", "葛",
            "伍", "韦", "申", "尤", "毕", "聂", "丛", "焦", "向", "柳",
            "邢", "路", "岳", "齐", "沿", "梅", "莫", "庄", "辛", "管",
            "祝", "左", "涂", "谷", "祁", "时", "舒", "耿", "牟", "卜",
            "路", "詹", "关", "苗", "凌", "费", "纪", "靳", "盛", "童",
            "欧", "甄", "项", "曲", "成", "游", "阳", "裴", "席", "卫",
            "查", "屈", "鲍", "位", "覃", "霍", "翁", "隋", "植", "甘",
            "景", "薄", "单", "包", "司", "柴", "瞿", "晏", "全", "郗",
            "秋", "仲", "伊", "宫", "宁", "仇", "栾", "暴", "厉", "戎",
            // 常见复姓
            "欧阳", "诸葛", "司马", "上官", "夏侯", "皇甫", "尉迟", "公孙",
            "慕容", "宇文", "长孙", "司徒", "司空", "南宫", "令狐", "端木",
            "东方", "太史", "呼延", "赫连"
    };

    // 名字首字库（120 个）
    private static final String[] GIVEN_CHARS_1 = {
            "伟", "芳", "娜", "秀", "敏", "静", "丽", "强", "磊", "军",
            "洋", "勇", "艳", "杰", "娟", "涛", "明", "超", "兰", "霞",
            "平", "刚", "桂", "文", "玲", "丹", "萍", "鹏", "华", "红",
            "飞", "龙", "浩", "辉", "雪", "晨", "宇", "博", "涵", "皓",
            "然", "轩", "睿", "泽", "凯", "越", "欣", "奕", "诺", "子",
            "梓", "思", "雨", "天", "嘉", "铭", "佳", "俊", "宏", "海",
            "清", "峰", "建", "振", "世", "维", "成", "远", "祥", "德",
            "志", "恒", "星", "阳", "帆", "舟", "语", "沐", "若", "锦",
            "书", "云", "宁", "安", "知", "行", "予", "可", "乐", "心",
            "琪", "瑶", "琳", "楚", "景", "琛", "煜", "昊", "宸", "润",
            "柏", "枫", "桐", "尧", "舜", "禹", "潇", "霖", "岩", "岳",
            "瀚", "澜", "渊", "泰", "康", "健", "盛", "昌", "隆", "达"
    };

    // 名字尾字库（120 个）
    private static final String[] GIVEN_CHARS_2 = {
            "轩", "宇", "涵", "泽", "睿", "皓", "晨", "洋", "博", "诺",
            "越", "欣", "凯", "杰", "涛", "明", "强", "刚", "华", "平",
            "文", "军", "磊", "鹏", "飞", "龙", "浩", "辉", "雪", "然",
            "丹", "萍", "红", "霞", "秀", "敏", "静", "丽", "芳", "娜",
            "娟", "英", "玲", "琪", "瑶", "琳", "楚", "景", "琛", "煜",
            "昊", "宸", "润", "柏", "枫", "桐", "尧", "禹", "潇", "霖",
            "岩", "岳", "瀚", "澜", "渊", "泰", "安", "宁", "康", "健",
            "盛", "昌", "隆", "兴", "达", "成", "功", "胜", "利", "顺",
            "和", "祥", "瑞", "丰", "源", "泉", "溪", "川", "洲", "海",
            "彬", "栋", "梁", "帆", "青", "雅", "韵", "菲", "萱", "晴",
            "宜", "真", "言", "希", "哲", "奇", "羽", "彤", "微", "曦",
            "恒", "星", "阳", "航", "舟", "锦", "书", "云", "意", "纯"
    };

    // 单字名库（80 个）
    private static final String[] SINGLE_GIVEN = {
            "伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "军", "洋",
            "勇", "艳", "杰", "娟", "涛", "明", "超", "霞", "平", "刚",
            "文", "玲", "丹", "萍", "鹏", "华", "红", "飞", "龙", "浩",
            "辉", "雪", "晨", "宇", "博", "涵", "皓", "然", "轩", "睿",
            "泽", "凯", "越", "欣", "诺", "峰", "斌", "帅", "楠", "巍",
            "波", "欢", "雷", "虎", "彪", "健", "威", "策", "锐", "锋",
            "航", "帆", "岩", "岳", "瀚", "达", "诚", "霖", "哲", "豪",
            "源", "泉", "洲", "洋", "川", "森", "枫", "桐", "松", "柏"
    };

    // 总名字空间：220 * (80 + 120 * 120) = 3,185,600 种不重复自然姓名
    private static final long TOTAL_NAME_SPACE = (long) SURNAMES.length *
            ((long) SINGLE_GIVEN.length + (long) GIVEN_CHARS_1.length * (long) GIVEN_CHARS_2.length);

    // 全制式手机号段前缀（覆盖移动、联通、电信、广电及虚拟运营商共 44 种号段）
    private static final String[] PHONE_PREFIXES = {
            // 中国移动
            "134", "135", "136", "137", "138", "139", "147", "150", "151", "152",
            "157", "158", "159", "172", "178", "182", "183", "184", "187", "188",
            "195", "197", "198",
            // 中国联通
            "130", "131", "132", "145", "155", "156", "166", "175", "176", "185",
            "186", "196",
            // 中国电信
            "133", "149", "153", "173", "177", "180", "181", "189", "190", "191",
            "193", "199",
            // 广电及虚拟运营商
            "192", "170", "171"
    };

    /**
     * 初始化加载系统中 WriteRom 已生成的联系人姓名，防止跨次运行或重装时重复
     */
    private static void ensureExistingNamesLoaded(@NonNull Context context) {
        if (!sExistingNamesLoaded) {
            synchronized (sLoadLock) {
                if (!sExistingNamesLoaded) {
                    try {
                        Uri uri = ContactsContract.Data.CONTENT_URI;
                        String[] projection = new String[]{ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME};
                        String selection = ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND " +
                                ContactsContract.Data.MIMETYPE + " = ?";
                        String[] selectionArgs = new String[]{
                                ACCOUNT_TYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                        };
                        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
                            if (cursor != null) {
                                while (cursor.moveToNext()) {
                                    String name = cursor.getString(0);
                                    if (name != null) {
                                        sExistingNames.add(name);
                                    }
                                }
                            }
                        }
                    } catch (SecurityException | IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                    sGlobalNameSeq.set(sExistingNames.size());
                    sExistingNamesLoaded = true;
                }
            }
        }
    }

    /**
     * 批量创建一批联系人并提交事务
     *
     * @param context    上下文
     * @param startIndex 起始全局索引
     * @param count      本次批次创建数量（建议 20~50 条）
     * @param callback   进度与中断控制
     * @return 实际成功创建的联系人数量
     */
    public static int createContactsBatch(@NonNull Context context,
                                          int startIndex,
                                          int count,
                                          @NonNull FileGenerator.ProgressCallback callback) {
        if (callback.shouldCancel() || count <= 0) {
            return 0;
        }

        while (callback.isPaused() && !callback.shouldCancel()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            }
        }

        if (callback.shouldCancel()) {
            return 0;
        }

        ensureExistingNamesLoaded(context);

        ArrayList<ContentProviderOperation> ops = new ArrayList<>(count * 5);

        for (int i = 0; i < count; i++) {
            if (callback.shouldCancel()) {
                break;
            }

            // 1. 生成保证 100% 绝对不重复的自然人名
            String surname;
            String givenName;
            String displayName;
            long seq;
            do {
                seq = sGlobalNameSeq.getAndIncrement();
                // 数学同余双射置换 (Coprime Multiplier A = 1000003, Offset C = 314159)
                long permuted = Math.abs(seq * 1000003L + 314159L) % TOTAL_NAME_SPACE;
                int surnameIndex = (int) (permuted % SURNAMES.length);
                int givenIndex = (int) (permuted / SURNAMES.length);

                surname = SURNAMES[surnameIndex];
                if (givenIndex < SINGLE_GIVEN.length) {
                    givenName = SINGLE_GIVEN[givenIndex];
                } else {
                    int dIdx = givenIndex - SINGLE_GIVEN.length;
                    String char1 = GIVEN_CHARS_1[dIdx / GIVEN_CHARS_2.length];
                    String char2 = GIVEN_CHARS_2[dIdx % GIVEN_CHARS_2.length];
                    givenName = char1 + char2;
                }
                displayName = NAME_PREFIX + surname + givenName;
            } while (!sExistingNames.add(displayName));

            int rawContactInsertIndex = ops.size();

            // 2. 创建 RawContact 实体，设置独立账户类型并禁用聚合以极大提升批处理插入速度
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
                    .withValue(ContactsContract.RawContacts.AGGREGATION_MODE, ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED)
                    .withYieldAllowed(true)
                    .build());

            // 3. 结构化姓名（带统一压测前缀，名与姓独立写入便于拼音检索）
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, surname)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                    .build());

            // 4. 多样化全号段电话号码（覆盖多运营商，8位伪随机后缀）
            long phoneSeed = Math.abs(seq * 7919L + (long) i * 104729L + (long) (Math.random() * 99999999L));
            String phonePrefix = PHONE_PREFIXES[(int) (phoneSeed % PHONE_PREFIXES.length)];
            long phoneSuffix = (phoneSeed / PHONE_PREFIXES.length) % 100000000L;
            String phoneNumber = String.format(Locale.US, "%s%08d", phonePrefix, phoneSuffix);

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            // 5. 工作邮箱
            String emailDomain = (i % 3 == 0) ? "writerom.com" : ((i % 3 == 1) ? "mobiiot-test.cn" : "stress-cloud.org");
            String email = String.format(Locale.US, "stress_%05d@%s", Math.abs(seq % 100000L), emailDomain);

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .build());

            // 6. 组织与备忘录 (丰富数据库字段与 WAL 日志量)
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE, "WriteRom 节点 · " + displayName)
                    .build());
        }

        if (ops.isEmpty()) {
            return 0;
        }

        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            // 估算每条联系人在 contacts2.db 及 WAL 日志中占据约 2KB 物理空间
            long deltaBytes = (long) count * 2048L;
            callback.onBytesWritten(deltaBytes);
            return count;
        } catch (OperationApplicationException | RemoteException | SecurityException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 一键彻底物理清除由 WriteRom 生成的所有测试联系人（绝不影响用户原有联系人）
     */
    public static int cleanAllContacts(@NonNull Context context) {
        ContentResolver resolver = context.getContentResolver();
        int deleted = 0;
        try {
            // 使用 CALLER_IS_SYNCADAPTER=true 强制物理硬删除，不留墓碑标记
            Uri uri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();

            String where = ContactsContract.RawContacts.ACCOUNT_TYPE + " = ?";
            String[] args = new String[]{ACCOUNT_TYPE};

            deleted = resolver.delete(uri, where, args);
            if (deleted == 0) {
                // 兼容性尝试标准 URI
                deleted = resolver.delete(ContactsContract.RawContacts.CONTENT_URI, where, args);
            }
        } catch (SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
        }

        // 清理本地姓名缓存
        synchronized (sLoadLock) {
            sExistingNames.clear();
            sExistingNamesLoaded = false;
            sGlobalNameSeq.set(0);
        }

        return deleted;
    }

    /**
     * 查询由 WriteRom 创建的现有测试联系人数量
     */
    public static int countExistingContacts(@NonNull Context context) {
        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.ACCOUNT_TYPE + " = ?",
                new String[]{ACCOUNT_TYPE},
                null)) {
            return cursor != null ? cursor.getCount() : 0;
        } catch (SecurityException | IllegalArgumentException e) {
            return 0;
        }
    }
}
