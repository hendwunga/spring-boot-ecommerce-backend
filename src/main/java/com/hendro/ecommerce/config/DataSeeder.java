package com.hendro.ecommerce.config;

import com.hendro.ecommerce.dao.ProductCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductCategoryRepository categoryRepository;
    private final DataSource dataSource;

    public DataSeeder(ProductCategoryRepository categoryRepository, DataSource dataSource) {
        this.categoryRepository = categoryRepository;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("db/seed-data.sql"));
        populator.execute(dataSource);
    }
}
