package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.pki.CredentialFormat;
import com.bettercloud.vault.api.pki.RoleOptions;
import com.bettercloud.vault.response.PkiResponse;
import com.bettercloud.vault.rest.RestResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.*;

public class PkiTests {

    @Before
    public void setup() throws VaultException {
        final String address = System.getProperty("VAULT_ADDR");
        final String appId = System.getProperty("VAULT_APP_ID");
        final String userId = System.getProperty("VAULT_USER_ID");

        assertNotNull(address);
        assertNotNull(appId);
        assertNotNull(userId);

        final String token = authenticate();
        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);

        final PkiResponse response = vault.pki().deleteRole("testRole");
        final RestResponse restResponse = response.getRestResponse();
        assertEquals(204, restResponse.getStatus());
    }

    @Test
    public void testCreateRole_Defaults() throws VaultException {
        final String token = authenticate();
        final String address = System.getProperty("VAULT_ADDR");
        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);

        vault.pki().createOrUpdateRole("testRole");
        final PkiResponse response = vault.pki().getRole("testRole");
        assertTrue(compareRoleOptions(new RoleOptions(), response.getRoleOptions()));
    }

    @Test
    public void testCreateRole_WithOptions() throws VaultException {
        final String token = authenticate();
        final String address = System.getProperty("VAULT_ADDR");
        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);

        final RoleOptions options = new RoleOptions().allowAnyName(true);
        vault.pki().createOrUpdateRole("testRole", options);
        final PkiResponse response = vault.pki().getRole("testRole");
        assertTrue(compareRoleOptions(options, response.getRoleOptions()));
    }

    @Test
    public void testDeleteRole() throws VaultException {
        final String token = authenticate();
        final String address = System.getProperty("VAULT_ADDR");
        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);

        testCreateRole_Defaults();
        final PkiResponse deleteResponse = vault.pki().deleteRole("testRole");
        assertEquals(204, deleteResponse.getRestResponse().getStatus());
        final PkiResponse getResponse = vault.pki().getRole("testRole");
        assertEquals(404, getResponse.getRestResponse().getStatus());
    }

    @Test
    public void testIssueCredential() throws VaultException, InterruptedException {
        final String token = authenticate();
        final String address = System.getProperty("VAULT_ADDR");
        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);

        // Create a role
        final PkiResponse createRoleResponse = vault.pki().createOrUpdateRole("testRole",
                new RoleOptions()
                        .allowedDomains(new ArrayList<String>(){{ add("myvault.com"); }})
                        .allowSubdomains(true)
                        .maxTtl("9h")
        );
        assertEquals(204, createRoleResponse.getRestResponse().getStatus());
        Thread.sleep(3000);

        // Issue cert
        final PkiResponse issueResponse = vault.pki().issue("testRole", "test.myvault.com", null, null, null, CredentialFormat.PEM);
        assertNotNull(issueResponse.getCredential().getCertificate());
        assertNotNull(issueResponse.getCredential().getPrivateKey());
        assertNotNull(issueResponse.getCredential().getSerialNumber());
        assertEquals("rsa", issueResponse.getCredential().getPrivateKeyType());
        assertNotNull(issueResponse.getCredential().getIssuingCa());
    }


    private String authenticate() throws VaultException {
        final String address = System.getProperty("VAULT_ADDR");
        final String userId = System.getProperty("VAULT_USER_ID");
        final String password = System.getProperty("VAULT_PASSWORD");

        assertNotNull(address);
        assertNotNull(userId);
        assertNotNull(password);

        final String path = "userpass/login/" + userId;
        final VaultConfig config = new VaultConfig(address);
        final Vault vault = new Vault(config);

        final String token = vault.auth().loginByUsernamePassword(path, password).getAuthClientToken();
        assertNotNull(token);
        assertNotSame("", token.trim());
        return token;
    }

    private boolean compareRoleOptions(final RoleOptions expected, final RoleOptions actual) {
        if (expected.getAllowAnyName() != null && !expected.getAllowAnyName().equals(actual.getAllowAnyName())) return false;
        if (expected.getAllowBareDomains() != null && !expected.getAllowBareDomains().equals(actual.getAllowBareDomains())) return false;
        if (expected.getAllowedDomains() != null) {
            if (!expected.getAllowedDomains().containsAll(actual.getAllowedDomains())
                    || !actual.getAllowedDomains().containsAll(expected.getAllowedDomains())) {
                return false;
            }
        }
        if (expected.getAllowIpSans() != null && !expected.getAllowIpSans().equals(actual.getAllowIpSans())) return false;
        if (expected.getAllowLocalhost() != null && !expected.getAllowLocalhost().equals(actual.getAllowLocalhost())) return false;
        if (expected.getAllowSubdomains() != null && !expected.getAllowSubdomains().equals(actual.getAllowSubdomains())) return false;
        if (expected.getClientFlag() != null && !expected.getClientFlag().equals(actual.getClientFlag())) return false;
        if (expected.getCodeSigningFlag() != null && !expected.getCodeSigningFlag().equals(actual.getCodeSigningFlag())) return false;
        if (expected.getEmailProtectionFlag() != null && !expected.getEmailProtectionFlag().equals(actual.getEmailProtectionFlag())) return false;
        if (expected.getKeyBits() != null && !expected.getKeyBits().equals(actual.getKeyBits())) return false;
        if (expected.getKeyType() != null && !expected.getKeyType().equals(actual.getKeyType())) return false;
        if (expected.getMaxTtl() != null && !expected.getMaxTtl().equals(actual.getMaxTtl())) return false;
        if (expected.getServerFlag() != null && !expected.getServerFlag().equals(actual.getServerFlag())) return false;
        if (expected.getTtl() != null && !expected.getTtl().equals(actual.getTtl())) return false;
        if (expected.getUseCsrCommonName() != null && !expected.getUseCsrCommonName().equals(actual.getUseCsrCommonName())) return false;
        return true;
    }

}
