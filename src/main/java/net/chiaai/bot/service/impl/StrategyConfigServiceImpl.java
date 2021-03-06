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
     * ??????StrategyConfig????????????
     */
    @Override
    public StrategyConfig getStrategyConfigWithLock(Long id) {
        //1.?????????????????????????????????
        LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
        //??????????????????
        strategyConfigQueryWrapper.eq(StrategyConfig::getId, id);
        strategyConfigQueryWrapper.last(" for update");
        return strategyConfigMapper.selectOne(strategyConfigQueryWrapper);
    }

    /**
     * ??????????????????
     */
    @Override
    public void createStrategyConfig(CreateStrategyRequest request) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        User user = userService.getUserByPhone(loginUser.getPhone());
        if (StringUtils.isBlank(user.getApiKey())) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "????????????API??????????????????");
        }
        //VIP????????????
        if (!user.getInviteUser()) {
            //???????????????
            if (request.getFirstPosition().compareTo(BigDecimal.TEN) >= 0 || request.getSteppingPosition().compareTo(BigDecimal.TEN) >= 0) {
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "??????????????????????????????????????????10????????????");
            }
            //1.???????????????????????????????????????
            LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
            //??????????????????(???STOP)
            strategyConfigQueryWrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
            strategyConfigQueryWrapper.eq(StrategyConfig::getUserId, loginUser.getId());
            List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(strategyConfigQueryWrapper);
            if (strategyConfigs.size() >= 2) {
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "???????????????????????????????????????????????????????????????");
            }
        }
        request.setSymbol(request.getSymbol().toUpperCase());
        //??????????????????
        //1.??????????????????????????????????????????
        LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
        //??????????????????(???STOP)
        strategyConfigQueryWrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
        strategyConfigQueryWrapper.ne(StrategyConfig::getLeverage, request.getLeverage());
        strategyConfigQueryWrapper.eq(StrategyConfig::getSymbol, request.getSymbol());
        strategyConfigQueryWrapper.eq(StrategyConfig::getUserId, loginUser.getId());
        List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(strategyConfigQueryWrapper);
        if (!CollectionUtils.isEmpty(strategyConfigs)) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "?????????????????????????????????,???????????????????????????????????????:" + request.getSymbol());
        }
        //1.?????????????????????
        //2.??????????????????
        binanceUtil.calcQuantity(request.getSymbol(), request.getLeverage().multiply(request.getFirstPosition()));
        //0.???????????????????????????????????????
        PositionSideResponse accountPositionSide = null;
        try {
            accountPositionSide = binanceClient.getPositionSide(user.getApiKey(), BinanceUtil.encodeParams(new BaseRequest(), user));
        } catch (Exception e) {
            log.error("??????????????????????????????", e);
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "?????????????????????????????????:" + e.getMessage());
        }
        if (!accountPositionSide.getDualSidePosition()) {
            try {
                //1.?????????????????????????????????
                BaseResponse dualPositionSideResp = binanceClient.updatePositionSide(user.getApiKey(), BinanceUtil.encodeParams(new PositionSideRequest(true), user));
                log.info("??????????????????????????????{}", JSON.toJSONString(dualPositionSideResp));
                if (dualPositionSideResp.getCode() != 200) {
                    throw new BizException(BizCodeEnum.OPERATION_FAILED, "?????????????????????????????????:" + dualPositionSideResp.getMsg());
                }
            } catch (Exception e) {
                log.error("?????????????????????????????????", e);
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "?????????????????????????????????:" + e.getMessage());
            }
        }
        //2.????????????????????????
        try {
            LeverageResponse leverageResp = binanceClient.leverage(user.getApiKey(), BinanceUtil.encodeParams(new LeverageRequest(request.getSymbol(), request.getLeverage().intValue()), user));
            log.info("?????????????????????{}", JSON.toJSONString(leverageResp));
            if (leverageResp.getCode() != 200) {
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "????????????????????????:" + leverageResp.getMsg());
            }
        } catch (Exception e) {
            log.error("????????????????????????", e);
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "????????????????????????:" + e.getMessage());
        }

        //????????????
        StrategyConfig strategyConfig = new StrategyConfig();
        BeanUtils.copyProperties(request, strategyConfig);
        strategyConfig.setUserId(loginUser.getId());
        strategyConfig.setStatus(StrategyStatusEnum.RUNNING);
        strategyConfig.setCreateTime(LocalDateTime.now());
        strategyConfig.setUpdateTime(LocalDateTime.now());
        strategyConfig.setRemark("");

        //?????????????????????????????????????????????
        if (request.getAutoSwitch()) {
            //??????K??? 21???
            CandleLineRequest candleLineRequest = new CandleLineRequest();
            candleLineRequest.setSymbol(strategyConfig.getSymbol());
            candleLineRequest.setLimit(21L);
            candleLineRequest.setInterval(IntervalTypeEnum.ONE_DAY.getCode());
            List<CandleLine> candleLines = binanceClient.candleLines(candleLineRequest);
            //??????boll???
            BollBand bollBand = IndicatorUtil.getCurrentBoll(candleLines);
            PositionSideEnum positionSide = this.calPositionSide(strategyConfig.getSymbol(), bollBand);
            strategyConfig.setPositionSide(positionSide);
        }
        strategyConfigMapper.insert(strategyConfig);
        dingTalkService.sendMessage(user,
                "?????????????????????" +
                        "\r\n????????????" + request.getSymbol() +
                        "\r\n???????????????" + DateUtils.format(LocalDateTime.now()));
    }

    @Override
    public ProfitVo countProfit(Long strategyConfigId) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        //????????????id?????????????????????position
        StrategyConfig strategyConfig = strategyConfigMapper.selectById(strategyConfigId);
        //?????????????????????share
        if (loginUser.getId() != 1 && !strategyConfig.getUserId().equals(loginUser.getId()) && !strategyConfig.getShare()) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "???????????????????????????????????????");
        }
        //????????????
        LambdaQueryWrapper<Position> positionQueryWrapper = new LambdaQueryWrapper<>();
        //??????????????????
        positionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
        List<Position> positions = positionMapper.selectList(positionQueryWrapper);
        //????????????
        BigDecimal price = binanceClient.price(strategyConfig.getSymbol()).getPrice();
        List<PositionInfo> positionInfos = positions.stream().map(x -> {
            PositionInfo positionInfo = new PositionInfo();
            BeanUtils.copyProperties(x, positionInfo);
            positionInfo.setPositionAmount(positionInfo.getPrice().multiply(positionInfo.getQuantity()).divide(strategyConfig.getLeverage(), 8, RoundingMode.HALF_DOWN));
            if (positionInfo.getStatus() == PositionStatusEnum.HOLD) {
                //??????????????????
                if (positionInfo.getPositionSide() == PositionSideEnum.LONG) {
                    //???????????????-??????????????? ?? ??????
                    positionInfo.setSlidingProfitAmount(price.subtract(positionInfo.getPrice()).multiply(positionInfo.getQuantity()));
                } else {
                    //???????????????-??????????????? ?? ??????
                    positionInfo.setSlidingProfitAmount(positionInfo.getPrice().subtract(price).multiply(positionInfo.getQuantity()));
                }
            }
            return positionInfo;
        }).collect(Collectors.toList());
        return new ProfitVo(strategyConfig.getSymbol(), positionInfos);
    }

    @Override
    public List<MaxFeeVo> maxFees() {
        //1.?????????????????????????????????
        LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
        //??????????????????
        strategyConfigQueryWrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
        List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(strategyConfigQueryWrapper);
        return strategyConfigs.stream().map(x -> {
            MaxFeeVo maxFeeVo = new MaxFeeVo();
            maxFeeVo.setSymbol(x.getSymbol());
            BigDecimal position = x.getFirstPosition();
            for (Integer i = 2; i <= x.getTimes(); i++) {
                //????????????????????? ?????? = ???????????? + ?????? ?? ?????????
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
            throw new BizException(BizCodeEnum.AUTH_FAILED, "???????????????????????????????????????");
        }
        if (strategyConfig.getStatus() == StrategyStatusEnum.STOP) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "?????????????????????????????????????????????");
        }
        //??????strategy??????
        strategyConfig.setRemark("");
        updateStatusExec(strategyConfig, request.getStatus());
    }


    private void updateStatusExec(StrategyConfig targetConfig, StrategyStatusEnum status) {
        targetConfig.setStatus(status);
        targetConfig.setUpdateTime(LocalDateTime.now());
        strategyConfigMapper.updateById(targetConfig);
        if (status == StrategyStatusEnum.STOP) {
            //??????????????????
            LambdaQueryWrapper<Position> positionQueryWrapper = new LambdaQueryWrapper<>();
            //??????????????????
            positionQueryWrapper.eq(Position::getStrategyConfigId, targetConfig.getId());
            positionQueryWrapper.eq(Position::getStatus, PositionStatusEnum.HOLD);
            List<Position> positions = positionMapper.selectList(positionQueryWrapper);
            //??????????????????
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
            throw new BizException(BizCodeEnum.AUTH_FAILED, "???????????????????????????????????????");
        }
        if (strategyConfig.getShare() == request.getShare() && strategyConfig.getStatus() == StrategyStatusEnum.STOP) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "?????????????????????????????????????????????");
        } else if (strategyConfig.getStatus() == StrategyStatusEnum.STOP) {
            //???????????????????????????????????????
            strategyConfig.setShare(request.getShare());
            strategyConfig.setUpdateTime(LocalDateTime.now());
            strategyConfigMapper.updateById(strategyConfig);
        } else {
            BeanUtils.copyProperties(request, strategyConfig);
            strategyConfig.setUpdateTime(LocalDateTime.now());
            strategyConfig.setRemark("");
            //?????????????????????????????????????????????
            if (request.getAutoSwitch()) {
                //??????K??? 21???
                CandleLineRequest candleLineRequest = new CandleLineRequest();
                candleLineRequest.setSymbol(strategyConfig.getSymbol());
                candleLineRequest.setLimit(21L);
                candleLineRequest.setInterval(IntervalTypeEnum.ONE_DAY.getCode());
                List<CandleLine> candleLines = binanceClient.candleLines(candleLineRequest);
                //??????boll???
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
        //?????????????????????????????????????????????
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
            throw new BizException(BizCodeEnum.PARAM_ERROR, "???????????????????????????");
        }
    }

    @Override
    public Page<Position> positions(Long strategyId, Integer pageNum, Integer pageSize, String column, String sort) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        //????????????id?????????????????????position
        StrategyConfig strategyConfig = strategyConfigMapper.selectById(strategyId);
        if (loginUser.getId() != 1 && !strategyConfig.getUserId().equals(loginUser.getId()) && !strategyConfig.getShare()) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "???????????????????????????????????????");
        }
        //????????????
        LambdaQueryWrapper<Position> positionQueryWrapper = new LambdaQueryWrapper<>();
        //??????????????????
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
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "????????????API??????????????????");
        }
        List<BalanceResponse> balance = binanceClient.balance(loginUser.getApiKey(), BinanceUtil.encodeParams(new BaseRequest(), user));
        return balance.stream().filter(x -> "USDT".equals(x.getAsset())).findAny().get();
    }

    @Override
    public AccountInfoResponse account() {
        BaseUser loginUser = SessionUtils.getLoginUser();
        User user = userService.getUserByPhone(loginUser.getPhone());
        if (StringUtils.isBlank(user.getApiKey())) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "????????????API??????????????????");
        }
        AccountInfoResponse account = binanceClient.account(loginUser.getApiKey(), BinanceUtil.encodeParams(new BaseRequest(), user));
        return account;
    }

    @Override
    public PositionSideEnum calPositionSide(String symbol, BollBand bollBand) {
        //bollBand.getMd(). divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))
        //??????????????????
        BigDecimal price = binanceClient.price(symbol).getPrice();
        if (price.compareTo(bollBand.getUpper()
                .add(bollBand.getMd().divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))) >= 0) {
            //??????????????? ?????? ????????? +???md/2??? ???????????????(????????????)
            return PositionSideEnum.SHORT;
        }
        if (price.compareTo(bollBand.getUpper()
                .add(bollBand.getMd())) < 0
                && price.compareTo(bollBand.getUpper()) >= 0) {
            //?????????????????? >= ?????? ??????????????????
            return PositionSideEnum.BOTH;
        } else if (price.compareTo(bollBand.getUpper()) < 0 &&
                price.compareTo(bollBand.getMiddle()
                        .add(bollBand.getMd())) >= 0) {
            //?????????????????? < ?????? ??? ?????? >= ?????? + ???md/2??? ????????????
            return PositionSideEnum.LONG;
        } else if (price.compareTo(bollBand.getMiddle()
                .add(bollBand.getMd())) < 0
                && price.compareTo(bollBand.getMiddle()
                .subtract(bollBand.getMd())) >= 0) {
            //?????? ?????? < ?????? + ???md/2??? ??? ?????? >= ?????? -???md/2?????????????????????
            return PositionSideEnum.BOTH;
        } else if (price.compareTo(bollBand.getMiddle()
                .subtract(bollBand.getMd())) < 0
                && price.compareTo(bollBand.getLower()) >= 0) {
            //?????? ?????? < ?????? - ???md/2?????? ?????? > ?????? ????????????
            return PositionSideEnum.SHORT;
        } else if (price.compareTo(bollBand.getLower()) < 0 &&
                price.compareTo(bollBand.getLower()
                        .subtract(bollBand.getMd())) >= 0) {
            //?????? ?????? < ?????? ??? ?????? >= ????????? - ???md/2)
            return PositionSideEnum.BOTH;
        } else if (price.compareTo(bollBand.getLower()
                .subtract(bollBand.getMd().divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))) < 0) {
            //??????????????? ?????? ????????? - ???md/2??? ???????????????(????????????)
            return PositionSideEnum.LONG;
        }
        log.error("??????????????????Position");
        return PositionSideEnum.BOTH;
    }

    @Override
    public void closePosition(Long positionId) {
        BaseUser loginUser = SessionUtils.getLoginUser();
        Position position = positionMapper.selectById(positionId);
        StrategyConfig strategyConfig = strategyConfigMapper.selectById(position.getStrategyConfigId());
        //????????????
        if (loginUser.getId() != 1 && !strategyConfig.getUserId().equals(loginUser.getId())) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "???????????????????????????????????????");
        }
        //????????????
        dealService.closePosition(strategyConfig, position, false);
    }

}
