package me.jianwen.mediask.api.tool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 明文密码加密工具测试。
 * <p>
 * 用法：
 * <pre>
 * ./scripts/m21.sh -pl mediask-api -Dtest=PasswordEncryptToolTest test
 * ./scripts/m21.sh -pl mediask-api -Dtest=PasswordEncryptToolTest \
 *   -DplainPassword=your_password test
 * </pre>
 */
class PasswordEncryptToolTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void shouldGenerateBcryptHashWhenGivenPlainPassword() {
        String plainPassword = System.getProperty("plainPassword", "jkl123");
        String encrypted = passwordEncoder.encode(plainPassword);

        System.out.println("plainPassword = " + plainPassword);
        System.out.println("encryptedPassword = " + encrypted);

        Assertions.assertTrue(passwordEncoder.matches(plainPassword, encrypted));
    }
}
