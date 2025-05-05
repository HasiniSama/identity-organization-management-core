/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.util;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.internal.CarbonContextDataHolder;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.sql.DataSource;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.mock;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.generateUniqueID;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;

public class TestUtils {

    public static final String DB_NAME = "testOrgMgt_db";
    public static final String H2_SCRIPT_NAME = "h2.sql";
    public static Map<String, BasicDataSource> dataSourceMap = new HashMap<>();

    private static final Calendar CALENDAR = Calendar.getInstance(TimeZone.getTimeZone(UTC));

    public static String getFilePath(String fileName) {

        if (StringUtils.isNotBlank(fileName)) {
            return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "dbscripts",
                    fileName).toString();
        }
        throw new IllegalArgumentException("DB Script file name cannot be empty.");
    }

    public static void initiateH2Base() throws Exception {

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("username");
        dataSource.setPassword("password");
        dataSource.setUrl("jdbc:h2:mem:test" + DB_NAME);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("select 1");
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().executeUpdate("RUNSCRIPT FROM '" + getFilePath(H2_SCRIPT_NAME) + "'");
        }
        dataSourceMap.put(DB_NAME, dataSource);
    }

    public static void closeH2Base() throws Exception {

        BasicDataSource dataSource = dataSourceMap.remove(DB_NAME);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public static Connection getConnection() throws SQLException {

        if (dataSourceMap.get(DB_NAME) != null) {
            return dataSourceMap.get(DB_NAME).getConnection();
        }
        throw new RuntimeException("No datasource initiated for database: " + DB_NAME);
    }

    public static void mockDataSource() throws Exception {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome,
                "repository/conf").toString());

        DataSource dataSource = dataSourceMap.get(DB_NAME);

        setStatic(DatabaseUtil.class.getDeclaredField("dataSource"), dataSource);

        Field carbonContextHolderField =
                CarbonContext.getThreadLocalCarbonContext().getClass().getDeclaredField("carbonContextHolder");
        carbonContextHolderField.setAccessible(true);
        CarbonContextDataHolder carbonContextHolder
                = (CarbonContextDataHolder) carbonContextHolderField.get(CarbonContext.getThreadLocalCarbonContext());
        carbonContextHolder.setUserRealm(mock(UserRealm.class));
        setStatic(Utils.class.getDeclaredField("dataSource"), dataSource);
    }

    public static void mockCarbonContext(String superOrgId) {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome,
                "repository/conf").toString());

        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SUPER_TENANT_DOMAIN_NAME);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(SUPER_TENANT_ID);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername("admin");
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUserId("1234");
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(superOrgId);
    }

    public static void storeAssociatedTenant(String tenantDomain, String orgId) throws Exception {

        try (Connection connection = getConnection()) {
            String sql = "INSERT INTO UM_TENANT (UM_TENANT_UUID, UM_DOMAIN_NAME, UM_CREATED_DATE, UM_USER_CONFIG, " +
                    "UM_ORG_UUID) VALUES ( ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, generateUniqueID());
            statement.setString(2, tenantDomain);
            statement.setTimestamp(3, Timestamp.from(Instant.now()), CALENDAR);
            statement.setBytes(4, new byte[10]);
            statement.setString(5, orgId);
            statement.execute();
        }
    }

    private static void setStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }
}
