package com.petmgmt.util;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import com.petmgmt.security.RoleAssignmentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Profile("demo")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final ReminderRepository reminderRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleAssignmentService roles;

    public DataInitializer(UserRepository userRepository, PetRepository petRepository,
                           HealthRecordRepository healthRecordRepository,
                           ReminderRepository reminderRepository, PasswordEncoder passwordEncoder,
                           RoleAssignmentService roles) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.reminderRepository = reminderRepository;
        this.passwordEncoder = passwordEncoder;
        this.roles = roles;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@petmgmt.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole("admin");
        admin.setIsAdmin(true);
        admin.setIsApproved(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(admin);
        roles.assign(admin.getId(), "admin");

        // Create demo user
        User demo = new User();
        demo.setUsername("demo");
        demo.setEmail("demo@petmgmt.com");
        demo.setPasswordHash(passwordEncoder.encode("demo123"));
        demo.setRole("user");
        demo.setIsApproved(true);
        demo.setCreatedAt(LocalDateTime.now());
        demo.setUpdatedAt(LocalDateTime.now());
        userRepository.save(demo);
        roles.assign(demo.getId(), "user");

        // Create demo pets
        Pet wangcai = new Pet();
        wangcai.setOwner(demo);
        wangcai.setName("旺财");
        wangcai.setSpecies("dog");
        wangcai.setBreed("金毛");
        wangcai.setGender("male");
        wangcai.setBirthDate(LocalDate.now().minusYears(2).minusMonths(6));
        wangcai.setWeight(BigDecimal.valueOf(28.0));
        wangcai.setColor("金色");
        petRepository.save(wangcai);

        Pet mimi = new Pet();
        mimi.setOwner(demo);
        mimi.setName("咪咪");
        mimi.setSpecies("cat");
        mimi.setBreed("英短");
        mimi.setGender("female");
        mimi.setBirthDate(LocalDate.now().minusYears(1).minusMonths(6));
        mimi.setWeight(BigDecimal.valueOf(4.2));
        mimi.setColor("蓝灰色");
        petRepository.save(mimi);

        // Create health records
        HealthRecord hr1 = new HealthRecord();
        hr1.setPet(wangcai); hr1.setCreator(demo); hr1.setRecordType("vaccine");
        hr1.setTitle("狂犬疫苗"); hr1.setRecordDate(LocalDate.now().minusMonths(6));
        hr1.setNextDueDate(LocalDate.now().plusMonths(6));
        hr1.setVeterinarian("张医生");
        healthRecordRepository.save(hr1);

        HealthRecord hr2 = new HealthRecord();
        hr2.setPet(wangcai); hr2.setCreator(demo); hr2.setRecordType("deworming");
        hr2.setTitle("体内驱虫"); hr2.setRecordDate(LocalDate.now().minusMonths(3));
        hr2.setNextDueDate(LocalDate.now().plusMonths(3));
        healthRecordRepository.save(hr2);

        HealthRecord hr3 = new HealthRecord();
        hr3.setPet(mimi); hr3.setCreator(demo); hr3.setRecordType("vaccine");
        hr3.setTitle("猫三联疫苗"); hr3.setRecordDate(LocalDate.now().minusMonths(4));
        hr3.setNextDueDate(LocalDate.now().plusMonths(8));
        hr3.setVeterinarian("李医生");
        healthRecordRepository.save(hr3);

        // Create reminders
        Reminder r1 = new Reminder();
        r1.setUser(demo); r1.setPet(wangcai); r1.setHealthRecord(hr1);
        r1.setTitle("狂犬疫苗接种到期"); r1.setReminderType("vaccine");
        r1.setReminderDate(LocalDate.now().plusMonths(6));
        reminderRepository.save(r1);

        Reminder r2 = new Reminder();
        r2.setUser(demo); r2.setPet(mimi); r2.setHealthRecord(hr3);
        r2.setTitle("猫三联疫苗加强"); r2.setReminderType("vaccine");
        r2.setReminderDate(LocalDate.now().plusMonths(8));
        reminderRepository.save(r2);

        System.out.println("Seed data initialized: admin/admin123 and demo/demo123");
    }
}
