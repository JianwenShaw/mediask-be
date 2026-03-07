package me.jianwen.mediask.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.jianwen.mediask.infra.security.JwtService;
import me.jianwen.mediask.service.application.dto.auth.AccessTokenPrincipalDTO;
import me.jianwen.mediask.service.application.AccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 权限控制集成测试
 * 用于验证不同用户权限访问接口的行为
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Disabled("Requires external dev MySQL/Redis environment; not suitable for default unit-test pipeline")
class PermissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AccessTokenService accessTokenService;

    // 测试用户数据
    private static final Long ADMIN_USER_ID = 1L;
    private static final Long SCHEDULE_ADMIN_USER_ID = 2L;
    private static final Long NORMAL_USER_ID = 100L;
    private static final Long DOCTOR_USER_ID = 200L;
    private static final Long PATIENT_USER_ID = 300L;

    private String adminToken;
    private String scheduleAdminToken;
    private String normalUserToken;
    private String doctorToken;
    private String patientToken;

    @BeforeEach
    void setUp() {
        // 生成不同权限的 JWT Token

        // 管理员：拥有所有权限
        adminToken = jwtService.generateAccessToken(
                ADMIN_USER_ID,
                "admin",
                1, // userTypeCode: 1=管理员
                List.of("admin", "schedule:query", "schedule:create", "schedule:update", "schedule:delete", "schedule:auto")
        ).token();

        // 排班管理员：拥有排班管理权限
        scheduleAdminToken = jwtService.generateAccessToken(
                SCHEDULE_ADMIN_USER_ID,
                "schedule_admin",
                2,
                List.of("schedule:query", "schedule:create", "schedule:update", "schedule:delete", "schedule:auto")
        ).token();

        // 普通用户：只有查询权限
        normalUserToken = jwtService.generateAccessToken(
                NORMAL_USER_ID,
                "normal_user",
                3,
                List.of("schedule:query")
        ).token();

        // 医生：只能查询排班（用于查看自己的排班）
        doctorToken = jwtService.generateAccessToken(
                DOCTOR_USER_ID,
                "doctor",
                4,
                List.of("schedule:query", "doctor:query")
        ).token();

        // 患者：只能查看可预约排班
        patientToken = jwtService.generateAccessToken(
                PATIENT_USER_ID,
                "patient",
                5,
                List.of("patient")
        ).token();
    }

    @Nested
    @DisplayName("分页查询排班接口 /api/v1/schedules")
    class ScheduleQueryTests {

        @Test
        @DisplayName("管理员应该可以访问分页查询排班接口")
        void admin_should_access_schedule_list() throws Exception {
            mockMvc.perform(get("/api/v1/schedules")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("拥有 schedule:query 权限的用户可以访问")
        void user_with_schedule_query_permission_should_access() throws Exception {
            mockMvc.perform(get("/api/v1/schedules")
                            .header("Authorization", "Bearer " + normalUserToken)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("排班管理员应该可以访问")
        void schedule_admin_should_access() throws Exception {
            mockMvc.perform(get("/api/v1/schedules")
                            .header("Authorization", "Bearer " + scheduleAdminToken)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("没有 schedule:query 权限的患者应该被拒绝")
        void patient_without_permission_should_be_denied() throws Exception {
            mockMvc.perform(get("/api/v1/schedules")
                            .header("Authorization", "Bearer " + patientToken)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        }

        @Test
        @DisplayName("未登录用户应该被拒绝")
        void unauthenticated_user_should_be_denied() throws Exception {
            mockMvc.perform(get("/api/v1/schedules")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("无效 Token 应该被拒绝")
        void invalid_token_should_be_denied() throws Exception {
            mockMvc.perform(get("/api/v1/schedules")
                            .header("Authorization", "Bearer invalid.token.here")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("JWT Token 解析测试")
    class JwtParseTests {

        @Test
        @DisplayName("应该能正确解析管理员 Token 的权限")
        void should_parse_admin_token_correctly() {
            AccessTokenPrincipalDTO principal = accessTokenService.parseAccessToken(adminToken).orElse(null);

            assert principal != null;
            assert principal.getUserId().equals(ADMIN_USER_ID);
            assert principal.getAuthorities().contains("schedule:query");
            assert principal.getAuthorities().contains("admin");
        }

        @Test
        @DisplayName("应该能正确解析普通用户 Token 的权限")
        void should_parse_normal_user_token_correctly() {
            AccessTokenPrincipalDTO principal = accessTokenService.parseAccessToken(normalUserToken).orElse(null);

            assert principal != null;
            assert principal.getUserId().equals(NORMAL_USER_ID);
            assert principal.getAuthorities().size() == 1;
            assert principal.getAuthorities().contains("schedule:query");
        }

        @Test
        @DisplayName("应该能正确解析患者 Token 的权限")
        void should_parse_patient_token_correctly() {
            AccessTokenPrincipalDTO principal = accessTokenService.parseAccessToken(patientToken).orElse(null);

            assert principal != null;
            assert principal.getUserId().equals(PATIENT_USER_ID);
            assert principal.getAuthorities().contains("patient");
            assert !principal.getAuthorities().contains("schedule:query");
        }
    }

    @Nested
    @DisplayName("调试工具：生成指定权限的 Token")
    class TokenGeneratorUtils {

        /**
         * 快捷方法：根据权限列表生成 Token
         * 使用方式：将需要测试的权限列表复制到 authorities 变量中
         */
        @Test
        @DisplayName("生成自定义权限的 Token（用于调试）")
        void generate_custom_token_for_debug() {
            // TODO: 根据需要修改权限列表
            List<String> customAuthorities = List.of(
                    "schedule:query",      // 查询排班权限
                    "schedule:create",     // 创建排班权限
                    "schedule:update",    // 更新排班权限
                    "schedule:delete",    // 删除排班权限
                    "schedule:auto"        // 自动排班权限
                    // 添加更多权限...
            );

            JwtService.JwtToken token = jwtService.generateAccessToken(
                    999L,                  // 自定义用户ID
                    "debug_user",
                    10,                    // 自定义用户类型
                    customAuthorities
            );

            System.out.println("========== 调试 Token ==========");
            System.out.println("User ID: 999");
            System.out.println("Authorities: " + customAuthorities);
            System.out.println("Token: " + token.token());
            System.out.println("================================");

            // 验证 Token 能被正确解析
            AccessTokenPrincipalDTO principal = tokenApplicationService.parseAccessToken(token.token()).orElse(null);
            assert principal != null;
            assert principal.getUserId().equals(999L);
            System.out.println("Token 解析成功！");
        }
    }
}
