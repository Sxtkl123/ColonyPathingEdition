package com.arxyt.colonypathingedition.core.config;

import com.arxyt.colonypathingedition.core.config.enums.BuilderModeEnum;
import net.minecraftforge.common.ForgeConfigSpec;

public class PathingConfig {
    public static ForgeConfigSpec.DoubleValue RAIL_COST_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue WATER_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue JUMP_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue DROP_COST_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue ROAD_COST_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue INSHAPE_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue DOORS_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue DIVE_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue CAVE_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue RAILEXIT_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue PANEL_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue SHINGLE_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue FARMLAND_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue LEAF_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue BERRY_COST_DEFINER;

    public static ForgeConfigSpec.DoubleValue ONRAIL_CALLBACK_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue ONROAD_CALLBACK_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue ONRAIL_PREFERENCE;
    public static ForgeConfigSpec.DoubleValue ONROAD_PREFERENCE;
    public static ForgeConfigSpec.IntValue CALLBACK_TIMES_TOLERANCE;

    public static ForgeConfigSpec.DoubleValue RESTAURANT_WAITING_TIME;

    public static ForgeConfigSpec.DoubleValue MAX_PERCENTAGE_HP_FOR_CURE;
    public static ForgeConfigSpec.DoubleValue MAX_HP_FOR_CURE;
    public static ForgeConfigSpec.IntValue HEAL_START;
    public static ForgeConfigSpec.IntValue HEAL_DURATION;

    public static ForgeConfigSpec.BooleanValue LUMBERJACK_WORK_WHEN_UNCONSTRUCTED;

    public static ForgeConfigSpec.EnumValue<BuilderModeEnum> BUILDER_MODE;
    public static ForgeConfigSpec.IntValue BUILDER_GIBBON_RANGE;
    public static ForgeConfigSpec.BooleanValue BUILDER_TAKE_ORDERS_EVERYWHERE;

    public static ForgeConfigSpec.BooleanValue PICK_MATERIAL_AT_HUT;
    public static ForgeConfigSpec.BooleanValue MAX_ANIMAL_MODIFIER;

    public static ForgeConfigSpec.IntValue MAX_PATHING_DISTANCE;

    public static ForgeConfigSpec init(ForgeConfigSpec.Builder builder) {
        builder.push("Pathing Cost Modifier #寻路Cost相关设置#");
        builder.push("Multiplier #乘子系数#");
        RAIL_COST_MULTIPLIER = builder
                .comment("Rail path cost multiplier (default: 0.1, original:0.1) #铁轨Cost乘数 (默认 : 0.1 殖民地原设置 : 0.1)#")
                .defineInRange("railCostMultiplier", 0.1, 0.0, 2.0);
        ROAD_COST_MULTIPLIER = builder
                .comment("Road path cost multiplier (default: 0.4, original:1/6) #路径Cost乘数 (默认 : 0.4 殖民地原设置 : 1/6)#")
                .defineInRange("roadCostMultiplier", 0.4, 0.0, 2.0);
        DROP_COST_MULTIPLIER = builder
                .comment("Drop cost multiplier (default: 1.0, original:1.0) #掉落Cost乘数 (默认 : 1.0 殖民地原设置 : 1.0)#\n" +
                        "Notice: The base formula for falling cost has been modified from |dY|³ to (|dY| - 2/5)³ - 8/125. #注意:下落Cost基础公式已经从|dY|³修改为(|dY|-2/5)³-8/125.#")
                .defineInRange("dropCostMultiplier", 1.0, 0.0, 10.0);
        builder.pop();
        builder.push("Basic Cost Definer #基础Cost定义#");
        WATER_COST_DEFINER = builder
                .comment("Water path cost addon (default: 8.0, original:2.0) #水路Cost (默认 : 8.0 殖民地原设置 : 2.0)#")
                .defineInRange("waterCostAddon", 8.0, 0.0, 24.0);
        JUMP_COST_DEFINER = builder
                .comment("Jump cost addon (default: 2.0, original:2.0) #跳跃(准确来说是非梯子爬升)Cost (默认 : 2.0 殖民地原设置 : 2.0)#")
                .defineInRange("jumpCostAddon", 2.0, 0.0, 24.0);
        INSHAPE_COST_DEFINER = builder
                .comment("In shape cost addon (default: 2.0, original:2.0) #实体方块内部行走的Cost (默认 : 2.0 殖民地原设置 : 2.0)#")
                .defineInRange("inShapeCostAddon", 2.0, 0.0, 24.0);
        DOORS_COST_DEFINER = builder
                .comment("Door and trapdoor’s cost addon (default: 3.0, original:3.0) #穿过各种\"门\"的Cost (默认 : 3.0 殖民地原设置 : 3.0)#")
                .defineInRange("doorsCostAddon", 3.0, 0.0, 24.0);
        DIVE_COST_DEFINER = builder
                .comment("Dive cost addon (default: 24.0, original:4.0) #潜水(潜多了会淹死)的Cost (默认 : 24.0 殖民地原设置 : 4.0)#")
                .defineInRange("diveCostAddon", 24.0, 0.0, 24.0);
        CAVE_COST_DEFINER = builder
                .comment("Breathing cave air cost addon (default: 0.3, original:3.0) #钻洞(人工的也算)的Cost,(默认 : 0.3 殖民地原设置 : 3.0)#")
                .defineInRange("caveCostAddon", 0.3, 0.0, 24.0);
        RAILEXIT_COST_DEFINER = builder
                .comment("Exit railway cost addon (default: 4.0, original:4.0) #下铁路(人工的也算)的Cost(探测铁轨为”站点“，不计cost) (默认 : 4.0 殖民地原设置 : 4.0)#")
                .defineInRange("railExitAddon", 4.0, 0.0, 24.0);
        builder.pop();
        builder.push("Typical Cost Definer #针对性Cost定义#");
        PANEL_COST_DEFINER = builder
                .comment("Walk into panel cost addon (default: 4.0) #走在面板方块中(会不停的跳)的Cost (默认 : 4.0)#")
                .defineInRange("panelCostAddon", 4.0, 0.0, 24.0);
        SHINGLE_COST_DEFINER = builder
                .comment("Walk on shingle cost addon (default: 3.0, original:3.0) #上房揭瓦(走在屋瓦上)的Cost (默认 : 3.0 殖民地原设置 : 3.0)#")
                .defineInRange("shingleCostAddon", 3.0, 0.0, 24.0);
        FARMLAND_COST_DEFINER = builder
                .comment("Jump in farmland or drop onto farmland cost addon (default: 8.0) #在农田中跳上跳下的Cost (默认 : 8.0)#")
                .defineInRange("farmlandCostAddon", 8.0, 0.0, 24.0);
        LEAF_COST_DEFINER = builder
                .comment("Walk on leaves cost addon (default: 4.0) #爬树(走在树叶上)的Cost (默认 : 4.0)#")
                .defineInRange("leafCostAddon", 4.0, 0.0, 24.0);
        BERRY_COST_DEFINER = builder
                .comment("Walk in sweet berry bush cost addon (default: 24.0) #被浆果丛扎的Cost (默认 : 24.0)#")
                .defineInRange("berryCostAddon", 24.0, 0.0, 24.0);
        builder.pop();
        builder.push("Advanced Pathfinding Constant Definer #进阶寻路算法常数定义(警告：慎重修改！会对全局寻路产生极大影响！)#");
        ONRAIL_CALLBACK_MULTIPLIER = builder
                .comment("""
                        When traversing callback nodes, there is a heuristic reduction on artificial roads. This value is used to control the rate at which the heuristic for railway callback nodes increases, and its formula is: RAIL_COST_MULTIPLIER * ONRAIL_CALLBACK_MULTIPLIER (default: 1.0)\s
                        走回头路(其节点暂定义为回扣节点)时在人工道路上会有启发值减免,这是用来控制铁路回扣节点启发值增加速度的数值，其算式为 RAIL_COST_MULTIPLIER * ONRAIL_CALLBACK_MULTIPLIER (默认 : 1.0)""")
                .defineInRange("onRailCallbackMutiplier", 1.0, 0.0, 2.0);
        ONROAD_CALLBACK_MULTIPLIER = builder
                .comment("""
                        This value is used to control the rate at which the heuristic for callback nodes on road blocks increases, and its formula is: ROAD_COST_MULTIPLIER * ONROAD_CALLBACK_MULTIPLIER (default: 1.2)\s
                        用来控制道路方块上回扣节点启发值增加速度的数值，其算式为 ROAD_COST_MULTIPLIER * ONROAD_CALLBACK_MULTIPLIER (默认 : 1.2)""")
                .defineInRange("onRoadCallbackMutiplier", 1.2, 0.0, 2.0);
        ONRAIL_PREFERENCE = builder
                .comment("""
                        This is a global heuristic reduction, representing how much villagers prefer railways when pathfinding far away from the target point. The smaller the value, the stronger the preference. (default: 0.85)
                        是一个全局的启发值减免，表现为村民在远离目标点处寻路时对铁路的信任程度，数值越小越信任(默认 : 0.85)""")
                .defineInRange("onRailPreference", 0.85, 0.5, 2.0);
        ONROAD_PREFERENCE = builder
                .comment("""
                        This is a global heuristic reduction, representing how much villagers prefer path blocks when pathfinding far away from the target point. The smaller the value, the stronger the preference. (default: 0.92)
                        是一个全局的启发值减免，表现为村民在远离目标点处寻路时对道路方块的信任程度，数值越小越信任(默认 : 0.92)""")
                .defineInRange("onRoadPreference", 0.92, 0.5, 2.0);
        CALLBACK_TIMES_TOLERANCE = builder
                .comment("Tolerates how many times callback nodes can be expanded during each pathfinding process. (default: 2) #能容忍每次寻路中回扣节点被扩展几次，数值过大可能会造成扩展的无用节点增加 (默认 : 2)#")
                .defineInRange("callbackTimesTolerance", 2, 1, 25);
        builder.pop();
        builder.pop();
        builder.push("Restaurant Related Modifier #餐厅相关逻辑修改#");
        RESTAURANT_WAITING_TIME= builder
                .comment("""
                        Duration citizens wait in the restaurant for cook service (minutes) (default: 0.5, original: 2.0)
                        你的村民在餐厅等待厨师服务的时间(分钟) (默认 : 0.5 殖民地原设置 : 2.0)""")
                .defineInRange("restaurantWaitingTime", 0.5, 0.0, 2.0);
        builder.pop();
        builder.push("Hospital Related Modifier #医院相关逻辑修改#");
        MAX_PERCENTAGE_HP_FOR_CURE= builder
                .comment("""
                        The percentage of HP at which your citizens will seek treatment. (default: 0.2, original: 0.0)
                        你的村民将在剩多少比例的HP时去寻求医生治疗 (默认 : 0.2 殖民地原设置 : 0.0)""")
                .defineInRange("curePercentageHP", 0.2, 0.0, 1.0);
        MAX_HP_FOR_CURE= builder
                .comment("""
                        The absolute amount of HP at which your citizens will seek treatment. (default: 10.0, original: 6.0)
                        你的村民将在剩多少HP时去寻求医生治疗 (默认 : 10.0 殖民地原设置 : 6.0)""")
                .defineInRange("cureHP", 10.0, 6.0, 40.0);
        HEAL_START = builder
                .comment("Citizen will start to heal themselves at that time. #市民开始自愈的时间(s)#")
                .defineInRange("startHeal", 480, 0, 4095);
        HEAL_DURATION = builder
                .comment("Citizen will randomly cure during this time. #市民将在这个时间段内均匀自愈(s)#")
                .defineInRange("healDuration", 2000, 0, 65535);
        builder.pop();
        builder.push("LumberJack Modifier #伐木工相关修改#");
        LUMBERJACK_WORK_WHEN_UNCONSTRUCTED = builder
                .comment("Lumberjcak will start to work only if hut is placed.\n 伐木工会在放置工作方块后立即开始工作 (功能在后期有些超模，建议仅在前期开启以分担少量工作量)。")
                .define("lumberjackWorkWhenUnconstructed",false);
        builder.pop();
        builder.push("Builder Mode Modifier #土木人修改#");
        BUILDER_MODE = builder
                .comment("""
                        Builder mode (default: NORMAL), optional below: 建筑工人模式, (默认: 常规)，可选项如下：
                        NORMAL: Normal mode, authentic. 常规: 默认选项，原汁原味的殖民地体验。
                        FORMALIST: Play as a formalist, jumping up and down on the construction site, but work when they just leave their hut. 形式主义者：像个形式主义者一样在工地上蹿下跳，但是会在离开土木小屋后立即开始工作。
                        SENTRY: Play as a sentry, stand at a stable position to build. 哨兵：像一个哨兵一样，站在工地的固定位置工作。
                        GOD: GOD SHOULD BUILD ANYWHERE THEY WANT. 创世神：神就应该想在哪儿干就在那儿干。
                        GIBBON: Play as a gibbon, jumping up and down with a large building range. 长臂猿：像猿猴一样上蹿下跳，但是只在一定建造范围内工作。""")
                .defineEnum("builderMode", BuilderModeEnum.NORMAL);
        BUILDER_GIBBON_RANGE = builder.comment("Building range of gibbon mode. 长臂猿模式下的建造范围。")
                        .defineInRange("builderGibbonRange", 20, 0, 128);
        BUILDER_TAKE_ORDERS_EVERYWHERE = builder.comment("Can builder take orders everywhere. 打灰人能否随时随地接单。")
                        .define("builderTakeOrdersEverywhere", true);
        builder.pop();
        builder.push("Common Citizens Modifier #通用市民修改#");
        MAX_ANIMAL_MODIFIER = builder
                .comment("Max animal modifier, would you like to modify the max animals to 2^(building level)? (default: false)\n 养殖场最大生物数是否改为 2^建筑等级 原为 2*建筑等级(动物数目过大会导致卡顿，所以不默认开启),(默认 : false)")
                .define("increaceMaxAnimal",false);
        PICK_MATERIAL_AT_HUT = builder.comment("Should citizens pick material at their own hut. 你的非快递员市民是否应当在他们的小屋方块处取货。")
                        .define("pickMaterialAtHut", true);
        builder.pop();
        builder.push("Basic Logic Modifier #基础逻辑修改#");
        MAX_PATHING_DISTANCE = builder
                .comment("Max pathing distance (default: 1000, original:500) 市民最大寻路距离,(默认 : 1000 殖民地原设置 : 500)")
                .defineInRange("pathingDistance", 1000, 500, 4095);
        builder.pop();
        return builder.build(); // 返回构建结果
    }
}