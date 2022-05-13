create database strategy_bot;

create table back_test
(
    id                  bigint auto_increment
        primary key,
    user_id             bigint                              not null comment '关联用户id',
    symbol              varchar(20)                         not null comment '交易对',
    start_time          datetime                            not null comment '开始时间',
    end_time            datetime                            not null comment '结束时间',
    position_side       varchar(10)                         not null comment 'BOTH("多空双开"),LONG("开多"),SHORT("开空")',
    leverage            int                                 null comment '杠杆倍率',
    times               int                                 not null comment '单向购买次数',
    first_position      int                                 not null comment '首仓金额(USDT)',
    stepping_position   int                                 not null comment '每仓步进金额(USDT)',
    target_rate         double(18, 10)                      not null comment '止盈率',
    target_shrinks_rate double(18, 10) default 0.0000000000 not null comment '止盈回撤率',
    cover_rate          double(18, 10)                      not null comment '补仓率',
    cover_shrinks_rate  double(18, 10) default 0.0000000000 not null comment '补仓回撤率',
    stop_rate           double(18, 10)                      null comment '止损率',
    auto_restart        tinyint(1)     default 0            not null comment '自动平仓后是否重建策略',
    auto_restart_level  int                                 null comment '自动平仓触发仓位',
    status              varchar(10)                         not null comment '状态',
    create_time         datetime                            not null comment '创建时间',
    update_time         datetime                            not null comment '更新时间'
);

create table back_test_result
(
    id             bigint auto_increment
        primary key,
    back_task_id   bigint                              not null comment '关联回测id',
    start_time     datetime                            not null comment '开始时间',
    end_time       datetime                            not null comment '结束时间',
    max_level      int                                 null comment '最大仓位',
    max_amount     double(18, 10)                      not null comment '最大持仓金额',
    times          int                                 not null comment '总仓位',
    total_profit   double(18, 10)                      not null comment '总盈亏金额',
    target_profit  double(18, 10) default 0.0000000000 not null comment '止盈金额',
    stop_profit    double(18, 10) default 0.0000000000 not null comment '止损金额',
    service_amount double(18, 10)                      not null comment '手续费',
    current_profit double(18, 10)                      not null comment '已实现盈亏金额',
    sliding_profit double(18, 10)                      not null comment '浮亏金额',
    status         varchar(10)                         not null comment '状态',
    create_time    datetime                            not null comment '创建时间',
    update_time    datetime                            not null comment '更新时间'
);

create table candle_line
(
    symbol     varchar(20)    not null,
    open_time  bigint         not null,
    open       double(18, 10) not null,
    high       double(18, 10) not null,
    low        double(18, 10) not null,
    close      double(18, 10) not null,
    volume     double(24, 10) null,
    close_time bigint         not null,
    constraint un_symbol_open_time
        unique (symbol, open_time)
);

create table orders
(
    id              bigint auto_increment
        primary key,
    position_id     bigint         not null comment '关联仓位id',
    symbol          varchar(20)    not null comment '交易对',
    position_side   varchar(10)    not null comment '持仓方向',
    side            varchar(10)    not null comment '买卖方向 SELL, BUY',
    type            varchar(16)    not null comment '订单类型 LIMIT, MARKET, STOP, TAKE_PROFIT, STOP_MARKET, TAKE_PROFIT_MARKET, TRAILING_STOP_MARKET',
    quantity        double(18, 10) not null comment '成交数量',
    price           double(18, 10) not null comment '成交价格',
    cum_quote       double(18, 10) not null comment '成交金额',
    service_amount  double(18, 10) not null comment '交易手续费',
    client_order_id varchar(100)   not null comment '用户自定义orderId',
    order_id        bigint         not null comment '币安订单号',
    status          varchar(16)    not null comment '订单状态',
    create_time     datetime       not null,
    update_time     datetime       not null
)
    comment '订单表';

create table position
(
    id                 bigint auto_increment
        primary key,
    strategy_config_id bigint         not null,
    level              int            not null comment '仓位等级',
    real_cover_rate    double(18, 10) not null comment '实际补仓率',
    position_side      varchar(10)    not null comment '持仓方向',
    quantity           double(18, 10) null comment '数量',
    price              double(18, 10) null comment '成本价',
    sell_price         double(18, 10) null comment '卖出单价',
    real_target_rate   double(18, 10) null comment '实际止盈率',
    service_amount     double(18, 10) not null comment '服务费',
    profit_amount      double(18, 10) null comment '盈亏金额',
    status             varchar(16)    not null comment '仓位状态',
    create_time        datetime       not null,
    update_time        datetime       not null
)
    comment '仓位表';

create index idx_strategy_config_id
    on position (strategy_config_id);

create table strategy_config
(
    id                  bigint auto_increment
        primary key,
    user_id             bigint               not null comment '关联用户id',
    symbol              varchar(20)          not null,
    auto_switch         tinyint(1) default 0 not null comment '是否自动切换方向',
    position_side       varchar(10)          not null comment 'BOTH("多空双开"),LONG("开多"),SHORT("开空")',
    leverage            int                  null comment '杠杆倍率',
    times               int                  not null comment '单向购买次数',
    first_position      double(18, 10)       not null comment '首仓金额(USDT)',
    stepping_position   double(18, 10)       not null comment '每仓步进金额(USDT)',
    target_rate         double(18, 10)       not null comment '止盈率',
    target_shrinks_rate double(18, 10)       not null comment '止盈回撤率',
    cover_rate          double(18, 10)       not null comment '补仓率',
    cover_shrinks_rate  double(18, 10)       not null comment '补仓回撤率',
    stop_rate           double(18, 10)       null comment '止损率',
    auto_restart        tinyint(1) default 0 not null comment '自动平仓后是否重建策略',
    auto_restart_level  int        default 0 not null comment '自动平仓触发仓位',
    status              varchar(10)          not null comment '状态',
    remark              varchar(500)         null comment '备注',
    share               tinyint(1) default 0 not null comment '是否分享策略',
    create_time         datetime             null,
    update_time         datetime             null
);

create table user
(
    id                  bigint auto_increment comment '用户ID'
        primary key,
    phone               varchar(20)                         not null comment '手机号',
    nick_name           varchar(50)                         not null comment '昵称',
    password            varchar(64)                         not null comment '密码',
    third_user_id       bigint                              null comment ' 三方用户ID',
    invite_user         tinyint(1)     default 0            not null comment 'VIP',
    email               varchar(100)                        null comment '邮箱',
    avatar              varchar(200)                        null comment '头像',
    service_amount_rate double(18, 10) default 0.0004000000 not null,
    api_key             varchar(128)                        null,
    api_secret          varchar(512)                        null,
    access_token        varchar(128)                        null comment '机器人授权码',
    disabled            tinyint                             not null comment '状态：0启用、1禁用',
    create_time         datetime                            not null comment '创建日期',
    update_time         datetime                            not null comment '更新时间'
);

