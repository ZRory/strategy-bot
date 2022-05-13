package net.chiaai.bot.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.*;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.common.utils.BinanceUtil;
import net.chiaai.bot.common.utils.DateUtils;
import net.chiaai.bot.common.utils.IndicatorUtil;
import net.chiaai.bot.common.utils.SessionUtils;
import net.chiaai.bot.common.web.BaseUser;
import net.chiaai.bot.entity.dao.CandleLine;
import net.chiaai.bot.entity.dao.Position;
import net.chiaai.bot.entity.dao.StrategyConfig;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.entity.dto.BollBand;
import net.chiaai.bot.entity.request.ChangeStrategyStatusRequest;
import net.chiaai.bot.entity.request.CreateStrategyRequest;
import net.chiaai.bot.entity.request.UpdateStrategyRequest;
import net.chiaai.bot.entity.response.MaxFeeVo;
import net.chiaai.bot.entity.response.PositionInfo;
import net.chiaai.bot.entity.response.ProfitVo;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.BaseRequest;
import net.chiaai.bot.feign.request.CandleLineRequest;
import net.chiaai.bot.feign.request.LeverageRequest;
import net.chiaai.bot.feign.request.PositionSideRequest;
import net.chiaai.bot.feign.response.*;
import net.chiaai.bot.mapper.PositionMapper;
import net.chiaai.bot.mapper.StrategyConfigMapper;
import net.chiaai.bot.service.DingTalkService;
import net.chiaai.bot.service.StrategyConfigService;
import net.chiaai.bot.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StrategyConfigServiceImpl implements StrategyConfigService {

    @Resource
    private BinanceFeignClient binanceClient;

    @Resource
    private PositionMapper positionMapper;

    @Resource
    private StrategyConfigMapper strategyConfigMapper;

    @Resource
    private DealServiceImpl dealService;

    @Resource
    private UserService userService;

    @Resource
    private BinanceUtil binanceUtil;

    @Resource
    private DingTalkService dingTalkService;

    /**
     * 获取StrategyConfig带悲观锁
     */
    @Override
    public StrategyConfig getStrategyConfigWithLock(Long id) {
        //1.查询所有在运行中的策略
        LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
        //封装查询条件
        strategyConfigQueryWrapper.eq(StrategyConfig::getId, id);
        strategyConfigQueryWrapper.last(" for update");
        return strategyConfigMapper.selectOne(strategyConfigQueryWrapper);
    }

    /**
     * 创建币种策略
     */
    @Override
    public void createStrategyConfig(CreateStrategyRequest request) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        User user = userService.getUserByPhone(loginUser.getPhone());
        if (StringUtils.isBlank(user.getApiKey())) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "请先配置API后再创建策略");
        }
        //VIP用户判断
        if (!user.getInviteUser()) {
            //非注册用户
            if (request.getFirstPosition().compareTo(BigDecimal.TEN) >= 0 || request.getSteppingPosition().compareTo(BigDecimal.TEN) >= 0) {
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "非邀请注册用户仅支持创建金额10以下策略");
            }
            //1.查询所有在运行中的币种策略
            LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
            //封装查询条件(非STOP)
            strategyConfigQueryWrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
            strategyConfigQueryWrapper.eq(StrategyConfig::getUserId, loginUser.getId());
            List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(strategyConfigQueryWrapper);
            if (strategyConfigs.size() >= 2) {
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "非邀请注册用户仅支持同时存在两个运行交易对");
            }
        }
        request.setSymbol(request.getSymbol().toUpperCase());
        //历史策略判定
        //1.查询所有在运行中的本币种策略
        LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
        //封装查询条件(非STOP)
        strategyConfigQueryWrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
        strategyConfigQueryWrapper.ne(StrategyConfig::getLeverage, request.getLeverage());
        strategyConfigQueryWrapper.eq(StrategyConfig::getSymbol, request.getSymbol());
        strategyConfigQueryWrapper.eq(StrategyConfig::getUserId, loginUser.getId());
        List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(strategyConfigQueryWrapper);
        if (!CollectionUtils.isEmpty(strategyConfigs)) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "存在运行中的相同币种时,新建策略应保持相同杠杆倍率:" + request.getSymbol());
        }
        //1.判断交易对信息
        //2.交易金额判断
        binanceUtil.calcQuantity(request.getSymbol(), request.getLeverage().multiply(request.getFirstPosition()));
        //0.查询持仓模式是否为双向持仓
        PositionSideResponse accountPositionSide = null;
        try {
            accountPositionSide = binanceClient.getPositionSide(user.getApiKey(), BinanceUtil.encodeParams(new BaseRequest(), user));
        } catch (Exception e) {
            log.error("查询账户双向持仓失败", e);
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "查询账户为双向持仓失败:" + e.getMessage());
        }
        if (!accountPositionSide.getDualSidePosition()) {
            try {
                //1.调整账户为双向持仓模式
                BaseResponse dualPositionSideResp = binanceClient.updatePositionSide(user.getApiKey(), BinanceUtil.encodeParams(new PositionSideRequest(true), user));
                log.info("调整账户为双向持仓：{}", JSON.toJSONString(dualPositionSideResp));
                if (dualPositionSideResp.getCode() != 200) {
                    throw new BizException(BizCodeEnum.OPERATION_FAILED, "调整账户为双向持仓失败:" + dualPositionSideResp.getMsg());
                }
            } catch (Exception e) {
                log.error("调整账户为双向持仓失败", e);
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "调整账户为双向持仓失败:" + e.getMessage());
            }
        }
        //2.调整币种杠杆倍率
        try {
            LeverageResponse leverageResp = binanceClient.leverage(user.getApiKey(), BinanceUtil.encodeParams(new LeverageRequest(request.getSymbol(), request.getLeverage().intValue()), user));
            log.info("调整杠杆倍率：{}", JSON.toJSONString(leverageResp));
            if (leverageResp.getCode() != 200) {
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "调整杠杆倍率失败:" + leverageResp.getMsg());
            }
        } catch (Exception e) {
            log.error("调整杠杆倍率失败", e);
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "调整杠杆倍率失败:" + e.getMessage());
        }

        //策略入库
        StrategyConfig strategyConfig = new StrategyConfig();
        BeanUtils.copyProperties(request, strategyConfig);
        strategyConfig.setUserId(loginUser.getId());
        strategyConfig.setStatus(StrategyStatusEnum.RUNNING);
        strategyConfig.setCreateTime(LocalDateTime.now());
        strategyConfig.setUpdateTime(LocalDateTime.now());
        strategyConfig.setRemark("");

        //如果是自动计算的则计算订单方向
        if (request.getAutoSwitch()) {
            //查询K线 21条
            CandleLineRequest candleLineRequest = new CandleLineRequest();
            candleLineRequest.setSymbol(strategyConfig.getSymbol());
            candleLineRequest.setLimit(21L);
            candleLineRequest.setInterval(IntervalTypeEnum.ONE_DAY.getCode());
            List<CandleLine> candleLines = binanceClient.candleLines(candleLineRequest);
            //计算boll值
            BollBand bollBand = IndicatorUtil.getCurrentBoll(candleLines);
            PositionSideEnum positionSide = this.calPositionSide(strategyConfig.getSymbol(), bollBand);
            strategyConfig.setPositionSide(positionSide);
        }
        strategyConfigMapper.insert(strategyConfig);
        dingTalkService.sendMessage(user,
                "操作：新建策略" +
                        "\r\n交易对：" + request.getSymbol() +
                        "\r\n操作时间：" + DateUtils.format(LocalDateTime.now()));
    }

    @Override
    public ProfitVo countProfit(Long strategyConfigId) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        //根据策略id查询策略下面的position
        StrategyConfig strategyConfig = strategyConfigMapper.selectById(strategyConfigId);
        //非本用户并且非share
        if (loginUser.getId() != 1 && !strategyConfig.getUserId().equals(loginUser.getId()) && !strategyConfig.getShare()) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "非本用户数据，禁止越权查看");
        }
        //查询空单
        LambdaQueryWrapper<Position> positionQueryWrapper = new LambdaQueryWrapper<>();
        //封装查询条件
        positionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
        List<Position> positions = positionMapper.selectList(positionQueryWrapper);
        //查询币价
        BigDecimal price = binanceClient.price(strategyConfig.getSymbol()).getPrice();
        List<PositionInfo> positionInfos = positions.stream().map(x -> {
            PositionInfo positionInfo = new PositionInfo();
            BeanUtils.copyProperties(x, positionInfo);
            positionInfo.setPositionAmount(positionInfo.getPrice().multiply(positionInfo.getQuantity()).divide(strategyConfig.getLeverage(), 8, RoundingMode.HALF_DOWN));
            if (positionInfo.getStatus() == PositionStatusEnum.HOLD) {
                //统计浮动盈亏
                if (positionInfo.getPositionSide() == PositionSideEnum.LONG) {
                    //（现在价格-开仓价格） × 数量
                    positionInfo.setSlidingProfitAmount(price.subtract(positionInfo.getPrice()).multiply(positionInfo.getQuantity()));
                } else {
                    //（开仓价格-现在价格） × 数量
                    positionInfo.setSlidingProfitAmount(positionInfo.getPrice().subtract(price).multiply(positionInfo.getQuantity()));
                }
            }
            return positionInfo;
        }).collect(Collectors.toList());
        return new ProfitVo(strategyConfig.getSymbol(), positionInfos);
    }

    @Override
    public List<MaxFeeVo> maxFees() {
        //1.查询所有在运行中的策略
        LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
        //封装查询条件
        strategyConfigQueryWrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
        List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(strategyConfigQueryWrapper);
        return strategyConfigs.stream().map(x -> {
            MaxFeeVo maxFeeVo = new MaxFeeVo();
            maxFeeVo.setSymbol(x.getSymbol());
            BigDecimal position = x.getFirstPosition();
            for (Integer i = 2; i <= x.getTimes(); i++) {
                //从第二仓开始算 二仓 = 首仓金额 + 仓位 × 步进值
                BigDecimal currentPositionLevel = new BigDecimal(i.toString());
                position = position.add(x.getFirstPosition().add(x.getSteppingPosition().multiply(currentPositionLevel)));
            }
            maxFeeVo.setRate(new BigDecimal(String.valueOf(x.getTimes())).multiply(x.getCoverRate()));
            maxFeeVo.setFee(position);
            return maxFeeVo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateStatus(ChangeStrategyStatusRequest request) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        StrategyConfig strategyConfig = strategyConfigMapper.selectById(request.getStrategyId());
        if (loginUser.getId() != 1 && !strategyConfig.getUserId().equals(loginUser.getId())) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "非本用户数据，禁止越权修改");
        }
        if (strategyConfig.getStatus() == StrategyStatusEnum.STOP) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "已结束策略无法修改，请重新创建");
        }
        //更新strategy状态
        strategyConfig.setRemark("");
        updateStatusExec(strategyConfig, request.getStatus());
    }


    private void updateStatusExec(StrategyConfig targetConfig, StrategyStatusEnum status) {
        targetConfig.setStatus(status);
        targetConfig.setUpdateTime(LocalDateTime.now());
        strategyConfigMapper.updateById(targetConfig);
        if (status == StrategyStatusEnum.STOP) {
            //卖出所有持仓
            LambdaQueryWrapper<Position> positionQueryWrapper = new LambdaQueryWrapper<>();
            //封装查询条件
            positionQueryWrapper.eq(Position::getStrategyConfigId, targetConfig.getId());
            positionQueryWrapper.eq(Position::getStatus, PositionStatusEnum.HOLD);
            List<Position> positions = positionMapper.selectList(positionQueryWrapper);
            //卖出所有单子
            for (Position position : positions) {
                dealService.closePosition(targetConfig, position, true);
            }
        }
    }

    @Override
    public void updateStrategyConfig(UpdateStrategyRequest request) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        StrategyConfig strategyConfig = strategyConfigMapper.selectById(request.getStrategyId());
        if (loginUser.getId() != 1 && !strategyConfig.getUserId().equals(loginUser.getId())) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "非本用户数据，禁止越权修改");
        }
        if (strategyConfig.getShare() == request.getShare() && strategyConfig.getStatus() == StrategyStatusEnum.STOP) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "已结束策略无法修改，请重新创建");
        } else if (strategyConfig.getStatus() == StrategyStatusEnum.STOP) {
            //停止状态只允许更改是否分享
            strategyConfig.setShare(request.getShare());
            strategyConfig.setUpdateTime(LocalDateTime.now());
            strategyConfigMapper.updateById(strategyConfig);
        } else {
            BeanUtils.copyProperties(request, strategyConfig);
            strategyConfig.setUpdateTime(LocalDateTime.now());
            strategyConfig.setRemark("");
            //如果是自动计算的则计算订单方向
            if (request.getAutoSwitch()) {
                //查询K线 21条
                CandleLineRequest candleLineRequest = new CandleLineRequest();
                candleLineRequest.setSymbol(strategyConfig.getSymbol());
                candleLineRequest.setLimit(21L);
                candleLineRequest.setInterval(IntervalTypeEnum.ONE_DAY.getCode());
                List<CandleLine> candleLines = binanceClient.candleLines(candleLineRequest);
                //计算boll值
                BollBand bollBand = IndicatorUtil.getCurrentBoll(candleLines);
                PositionSideEnum positionSide = this.calPositionSide(strategyConfig.getSymbol(), bollBand);
                strategyConfig.setPositionSide(positionSide);
            }
            strategyConfigMapper.updateById(strategyConfig);
        }
    }

    @Override
    public Page<StrategyConfig> list(Integer pageNum, Integer pageSize, String symbol, String status, Boolean share, String column, String sort) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        LambdaQueryWrapper<StrategyConfig> strategyQueryWrapper = new LambdaQueryWrapper<>();
        if (share) {
            strategyQueryWrapper.eq(StrategyConfig::getShare, true);
        } else {
            if (loginUser.getId() != 1) {
                strategyQueryWrapper.eq(StrategyConfig::getUserId, loginUser.getId());
            }
        }
        if (StringUtils.isNotBlank(symbol)) {
            strategyQueryWrapper.eq(StrategyConfig::getSymbol, symbol);
        }
        if (StringUtils.isNotBlank(status)) {
            strategyQueryWrapper.eq(StrategyConfig::getStatus, status);
        }
        if (StringUtils.isNotBlank(column) && StringUtils.isNotBlank(sort)) {
            strategyQueryWrapper.last("ORDER BY " + getColumnByField(StrategyConfig.class, column) + " " + sort);
        } else {
            strategyQueryWrapper.last("ORDER BY FIELD(status,'ERROR','PAUSE','RUNNING','STOP') ASC,id ASC");
        }
        return strategyConfigMapper.selectPage(new Page<>(pageNum, pageSize), strategyQueryWrapper);
    }

    protected String getColumnByField(Class<?> clazz, String fieldName) {
        //通过反射获取数据类的属性及注解
        try {
            Field field = clazz.getDeclaredField(fieldName);
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null) {
                return tableField.value();
            }
            TableId tableId = field.getAnnotation(TableId.class);
            if (tableId != null) {
                return tableId.value();
            }
            return fieldName;
        } catch (NoSuchFieldException e) {
            throw new BizException(BizCodeEnum.PARAM_ERROR, "要排序的字段不存在");
        }
    }

    @Override
    public Page<Position> positions(Long strategyId, Integer pageNum, Integer pageSize, String column, String sort) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        //根据策略id查询策略下面的position
        StrategyConfig strategyConfig = strategyConfigMapper.selectById(strategyId);
        if (loginUser.getId() != 1 && !strategyConfig.getUserId().equals(loginUser.getId()) && !strategyConfig.getShare()) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "非本用户数据，禁止越权查看");
        }
        //查询空单
        LambdaQueryWrapper<Position> positionQueryWrapper = new LambdaQueryWrapper<>();
        //封装查询条件
        positionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
        //positionQueryWrapper.orderByDesc(Position::getStatus);
        if (StringUtils.isNotBlank(column) && StringUtils.isNotBlank(sort)) {
            positionQueryWrapper.last("ORDER BY " + getColumnByField(Position.class, column) + " " + sort);
        } else {
            List<SFunction<Position, ?>> columns = new ArrayList<>();
            columns.add(Position::getStatus);
            columns.add(Position::getUpdateTime);
            positionQueryWrapper.orderByDesc(true, columns);
        }
        return positionMapper.selectPage(new Page<>(pageNum, pageSize), positionQueryWrapper);
    }

    @Override
    public BalanceResponse balance() {
        BaseUser loginUser = SessionUtils.getLoginUser();
        User user = userService.getUserByPhone(loginUser.getPhone());
        if (StringUtils.isBlank(user.getApiKey())) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "请先配置API后再查看数据");
        }
        List<BalanceResponse> balance = binanceClient.balance(loginUser.getApiKey(), BinanceUtil.encodeParams(new BaseRequest(), user));
        return balance.stream().filter(x -> "USDT".equals(x.getAsset())).findAny().get();
    }

    @Override
    public AccountInfoResponse account() {
        BaseUser loginUser = SessionUtils.getLoginUser();
        User user = userService.getUserByPhone(loginUser.getPhone());
        if (StringUtils.isBlank(user.getApiKey())) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "请先配置API后再查看数据");
        }
        AccountInfoResponse account = binanceClient.account(loginUser.getApiKey(), BinanceUtil.encodeParams(new BaseRequest(), user));
        return account;
    }

    @Override
    public PositionSideEnum calPositionSide(String symbol, BollBand bollBand) {
        //bollBand.getMd(). divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))
        //查询当前币价
        BigDecimal price = binanceClient.price(symbol).getPrice();
        if (price.compareTo(bollBand.getUpper()
                .add(bollBand.getMd().divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))) >= 0) {
            //上插针的话 超过 上轨道 +（md/2） 就不做多了(防止被套)
            return PositionSideEnum.SHORT;
        }
        if (price.compareTo(bollBand.getUpper()
                .add(bollBand.getMd())) < 0
                && price.compareTo(bollBand.getUpper()) >= 0) {
            //如果当前币价 >= 上轨 （多空双向）
            return PositionSideEnum.BOTH;
        } else if (price.compareTo(bollBand.getUpper()) < 0 &&
                price.compareTo(bollBand.getMiddle()
                        .add(bollBand.getMd())) >= 0) {
            //如果当前币价 < 上轨 且 币价 >= 中轨 + （md/2） （多向）
            return PositionSideEnum.LONG;
        } else if (price.compareTo(bollBand.getMiddle()
                .add(bollBand.getMd())) < 0
                && price.compareTo(bollBand.getMiddle()
                .subtract(bollBand.getMd())) >= 0) {
            //如果 币价 < 中轨 + （md/2） 且 币价 >= 中轨 -（md/2）（多空双向）
            return PositionSideEnum.BOTH;
        } else if (price.compareTo(bollBand.getMiddle()
                .subtract(bollBand.getMd())) < 0
                && price.compareTo(bollBand.getLower()) >= 0) {
            //如果 币价 < 中轨 - （md/2）且 币价 > 下轨 （空向）
            return PositionSideEnum.SHORT;
        } else if (price.compareTo(bollBand.getLower()) < 0 &&
                price.compareTo(bollBand.getLower()
                        .subtract(bollBand.getMd())) >= 0) {
            //如果 币价 < 下轨 且 币价 >= 下轨道 - （md/2)
            return PositionSideEnum.BOTH;
        } else if (price.compareTo(bollBand.getLower()
                .subtract(bollBand.getMd().divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))) < 0) {
            //下插针的话 超过 下轨道 - （md/2） 就不做空了(防止被套)
            return PositionSideEnum.LONG;
        }
        log.error("未找到适配的Position");
        return PositionSideEnum.BOTH;
    }

    @Override
    public void closePosition(Long positionId) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        Position position = positionMapper.selectById(positionId);
        StrategyConfig strategyConfig = strategyConfigMapper.selectById(position.getStrategyConfigId());
        //权限校验
        if (loginUser.getId() != 1 && !strategyConfig.getUserId().equals(loginUser.getId())) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "非本用户数据，禁止越权操作");
        }
        //关闭仓位
        dealService.closePosition(strategyConfig, position, false);
    }

}
