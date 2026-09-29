package com.petmgmt.util;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import com.petmgmt.security.RoleAssignmentService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fictitious data for the isolated, in-memory demonstration profile only. */
@Component
@Profile("demo")
public class DemoCatalogInitializer {
    private final UserRepository users;
    private final ProductRepository products;
    private final HospitalRepository hospitals;
    private final PasswordEncoder passwords;
    private final RoleAssignmentService roles;

    public DemoCatalogInitializer(UserRepository users, ProductRepository products,
            HospitalRepository hospitals, PasswordEncoder passwords, RoleAssignmentService roles) {
        this.users = users; this.products = products; this.hospitals = hospitals;
        this.passwords = passwords; this.roles = roles;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        User merchant = users.findByUsername("demo_shop").orElseGet(() -> {
            User user = User.builder().username("demo_shop").email("demo-shop@example.invalid")
                .passwordHash(passwords.encode("demoShop123")).role("merchant")
                .storeName("毛孩子生活馆（演示）").isApproved(true).build();
            users.save(user); roles.assign(user.getId(), "merchant"); return user;
        });
        if (products.count() == 0) {
            products.save(Product.builder().name("成犬日常粮（演示商品）").category("food")
                .targetSpecies("dog").description("用于展示商城下单流程，不构成购买推荐。")
                .price(89.0).stock(50).merchant(merchant).build());
            products.save(Product.builder().name("互动玩具球（演示商品）").category("toy")
                .targetSpecies("both").description("猫狗通用的演示商品，供库存和购物车测试。")
                .price(25.0).stock(30).merchant(merchant).build());
            products.save(Product.builder().name("猫咪日常粮（演示商品）").category("food")
                .targetSpecies("cat").description("示例商品数据，仅在 demo 环境生成。")
                .price(69.0).stock(40).merchant(merchant).build());
        }
        if (hospitals.count() == 0) {
            Hospital hospital = new Hospital();
            hospital.setName("演示宠物医院（虚构数据）");
            hospital.setAddress("示例地址，仅用于地图和评价功能演示");
            hospital.setLatitude(31.2304); hospital.setLongitude(121.4737);
            hospital.setServices("体检、疫苗、健康咨询（演示）");
            hospital.setHours("09:00—18:00（示例）");
            hospitals.save(hospital);
        }
    }
}
