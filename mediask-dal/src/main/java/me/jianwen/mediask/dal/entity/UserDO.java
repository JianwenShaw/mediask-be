package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.GenderEnum;
import me.jianwen.mediask.dal.enums.UserTypeEnum;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("users")
public class UserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * AES加密身份证号(Base64编码)
     */
    private String idCardEncrypted;

    /**
     * BCrypt加密密码
     */
    private String password;

    /**
     * 用户类型
     */
    private UserTypeEnum userType;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 性别
     */
    private GenderEnum gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
