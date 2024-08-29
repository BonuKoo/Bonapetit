package com.eatmate.db.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.eatmate.dao.repository"
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
@MapperScan(
        sqlSessionFactoryRef = "sessionFactory",
        value = {
                "com.eatmate.dao.mybatis"
        }
)
public class DBConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean managefactoryBean = new LocalContainerEntityManagerFactoryBean();
        managefactoryBean.setDataSource(dataSource());
        managefactoryBean.setPersistenceUnitName("persistence");
        managefactoryBean.setPackagesToScan(
                "com.eatmate.domain.entity",
                "com.eatmate.global.domain"
        );

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();

        //DDL 생성 기능 활성화
        vendorAdapter.setGenerateDdl(true);
        managefactoryBean.setJpaVendorAdapter(vendorAdapter);

        HashMap<String, Object> jpaProperties = new HashMap<>();

        //YML에 설정해둔 ddl-auto 보다 이게 우선순위가 높다.
        //jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        managefactoryBean.setJpaPropertyMap(jpaProperties);

        return managefactoryBean;
    }

    @Primary
    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Primary
    @Bean(name = "sessionFactory")
    public SqlSessionFactory sqlSessionFactory(ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource());
        sqlSessionFactoryBean.setTypeAliasesPackage("com.eatmate.domain.dto");
        return sqlSessionFactoryBean.getObject();
    }
}
