package com.youlai.boot.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.youlai.boot.app.model.dto.AppUserAuthInfo;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.app.mapper.AppUserMapper;
import com.youlai.boot.app.service.AppUserService;
import com.youlai.boot.app.model.entity.AppUser;
import com.youlai.boot.app.model.form.AppUserForm;
import com.youlai.boot.app.model.query.AppUserQuery;
import com.youlai.boot.app.model.vo.AppUserVO;
import com.youlai.boot.app.converter.AppUserConverter;

import java.util.Arrays;
import java.util.List;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 *
 * @author yuyu
 * @since 2025-02-20 22:03
 */
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    private final AppUserConverter appUserConverter;

    /**
    * 获取用户分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<AppUserVO>} 用户分页列表
    */
    @Override
    public IPage<AppUserVO> getAppUserPage(AppUserQuery queryParams) {
        Page<AppUserVO> pageVO = this.baseMapper.getAppUserPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取用户表单数据
     *
     * @param id 用户ID
     * @return
     */
    @Override
    public AppUserForm getAppUserFormData(Long id) {
        AppUser entity = this.getById(id);
        return appUserConverter.toForm(entity);
    }
    
    /**
     * 新增用户
     *
     * @param formData 用户表单对象
     * @return
     */
    @Override
    public boolean saveAppUser(AppUserForm formData) {
        AppUser entity = appUserConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新用户
     *
     * @param id   用户ID
     * @param formData 用户表单对象
     * @return
     */
    @Transactional
    @Override
    public boolean updateAppUser(Long id,AppUserForm formData) {
        if(formData.getStudentId() == null) formData.setStudentId(id);
        //如果不是BCrypt加密，则加密
        if(formData.getPassword() != null && !isBCrypt(formData.getPassword())) {
            formData.setPassword(new BCryptPasswordEncoder().encode(formData.getPassword()));
        }
        if(formData.getAuthInfo() == null){
            UpdateWrapper<AppUser> updateWrapper = new UpdateWrapper<>();
            updateWrapper
                    .eq("student_id", id)
                    .set("auth_info", null)
                    .set("delete_url", null);
            this.update(null, updateWrapper);
        }
        AppUser entity = appUserConverter.toEntity(formData);
        return this.updateById(entity);
    }
    // 判断字符串是否符合BCrypt的格式
    public static boolean isBCrypt(String password) {
        // 检查密码长度和格式
        return password != null && password.matches("^\\$2[ayb]\\$\\d{2}\\$[A-Za-z0-9./]{53}$");
    }
    /**
     * 删除用户
     *
     * @param ids 用户ID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteAppUsers(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的用户数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    /**
     * 根据学号获取用户信息
     * @param studentId
     * @return
     */
    @Override
    public AppUserAuthInfo getUserAuthInfo(String studentId) {
        AppUserAuthInfo userAuthInfo = this.baseMapper.getUserAuthInfo(studentId);
//        if(userAuthInfo != null) {
//            String password = userAuthInfo.getPassword();
//            password = new BCryptPasswordEncoder().encode(password);
//            userAuthInfo.setPassword(password);
//        }
        return userAuthInfo;
    }
    @Override
    public void register(AppUserForm appUserForm) {
        appUserForm
                .setPassword(new BCryptPasswordEncoder().encode(appUserForm.getPassword()));
        // 1. 根据学号检查，是否已经注册 或 已经申请过注册
        AppUserAuthInfo appUserAuthInfo = getUserAuthInfo(String.valueOf(appUserForm.getStudentId()));
        if(appUserAuthInfo != null) {
            // 已提交注册申请，请勿重复注册
            if(appUserAuthInfo.getAuthStatus() == 0) throw new BusinessException(ResultCode.REGISTRATION_HAS_APPLIED);
            // 已经注册，请勿重复注册
            else throw new BusinessException(ResultCode.USER_ALREADY_REGISTERED);
        }
        // 2.未注册过或未提交过申请，进行注册
        this.baseMapper.insert(appUserConverter.toEntity(appUserForm));
    }

}
