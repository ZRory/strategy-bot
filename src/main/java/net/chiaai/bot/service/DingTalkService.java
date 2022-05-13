package net.chiaai.bot.service;


import net.chiaai.bot.common.web.BaseUser;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.entity.request.GetMsgCodeRequest;
import net.chiaai.bot.entity.request.UserKeyRequest;
import net.chiaai.bot.entity.request.UserLoginRequest;
import net.chiaai.bot.entity.request.UserModifyRequest;
import net.chiaai.bot.entity.response.GraphCodeResponse;

public interface DingTalkService {

    void sendMessage(User user, String message);

}