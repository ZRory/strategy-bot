package net.chiaai.bot.service;


import net.chiaai.bot.common.web.BaseUser;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.entity.request.*;
import net.chiaai.bot.entity.response.GraphCodeResponse;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public interface UserService {

    void register(UserLoginRequest request);

    void modifyPassword(UserModifyRequest request);

    void findBackPassword(UserLoginRequest request);

    BaseUser login(UserLoginRequest request);

    GraphCodeResponse generateGraphCode();

    void getSmsCode(GetMsgCodeRequest request);

    void updateAPI(UserKeyRequest request) throws Exception;

    User getUserByPhone(String phone);

    void updateUser(UpdateUserRequest user);

    BaseUser flushUserInfo();
}