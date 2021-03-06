package org.nutz.boot.starter.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.nutz.boot.annotation.PropDoc;
import org.nutz.dao.impl.SimpleDataSource;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@IocBean
public class DataSourceStarter {

	protected static String PRE = "jdbc.";
	@PropDoc(group = "jdbc", value = "连接池类型", possible = { "druid", "simple", "hikari" }, defaultValue = "druid")
	public static final String PROP_TYPE = PRE + "type";
	@PropDoc(group = "jdbc", value = "JDBC URL", need = true)
	public static final String PROP_URL = PRE + "url";
	@PropDoc(group = "jdbc", value = "数据库用户名")
	public static final String PROP_USERNAME = PRE + "username";
	@PropDoc(group = "jdbc", value = "数据库密码")
	public static final String PROP_PASSWORD = PRE + "password";

	@Inject
	protected PropertiesProxy conf;

	@Inject("refer:$ioc")
	protected Ioc ioc;

	@IocBean
	public DataSource getDataSource() throws Exception {
		switch (conf.get(PROP_TYPE, "druid")) {
		case "simple":
		case "org.nutz.dao.impl.SimpleDataSource":
			SimpleDataSource simpleDataSource = new SimpleDataSource();
			String jdbcUrl = conf.get(PRE + "jdbcUrl", conf.get(PRE + "url"));
			if (Strings.isBlank(jdbcUrl)) {
				throw new RuntimeException("need " + PRE + ".url");
			}
			simpleDataSource.setJdbcUrl(jdbcUrl);
			simpleDataSource.setUsername(conf.get(PROP_USERNAME));
			simpleDataSource.setPassword(conf.get(PROP_PASSWORD));
			return simpleDataSource;
		case "druid":
		case "com.alibaba.druid.pool.DruidDataSource":
			return ioc.get(DataSource.class, "druidDataSource");
		case "hikari":
		case "com.zaxxer.hikari.HikariDataSource":
			return ioc.get(DataSource.class, "hikariDataSource");
		default:
			break;
		}
		throw new RuntimeException("not supported jdbc.type=" + conf.get("jdbc.type"));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@IocBean(name = "druidDataSource", depose = "close")
	public DataSource createDruidDataSource() throws Exception {
		if (Strings.isBlank(conf.get(PROP_URL))) {
			throw new RuntimeException("need jdbc.url");
		}
		Map map = Lang.filter(new HashMap(conf.toMap()), PRE, null, null, null);
		DruidDataSource dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(map);
		if (!conf.has(PRE + ".filters"))
			dataSource.setFilters("stat");
		return dataSource;
	}
	
	@IocBean(name = "hikariDataSource", depose = "close")
	public DataSource createHikariCPDataSource() throws Exception {
		if (Strings.isBlank(conf.get(PROP_URL))) {
			throw new RuntimeException("need jdbc.url");
		}
		Properties properties = new Properties();
		for (String key : conf.keys()) {
			if (!key.startsWith("jdbc.") || key.equals("jdbc.type"))
				continue;
			if (key.equals("jdbc.url")) {
				if (!conf.has("jdbc.jdbcUrl"))
					properties.put("jdbcUrl", conf.get(key));
			}
			else {
				properties.put(key.substring(5), conf.get(key));
			}
		}
		return new HikariDataSource(new HikariConfig(properties));
	}

	protected static boolean isDruid(PropertiesProxy conf) {
		String type = conf.get(PROP_TYPE, "druid");
		return "druid".equals(type) || "com.alibaba.druid.pool.DruidDataSource".equals(type);
	}
}
