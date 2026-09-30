package com.petmgmt.util;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** Editable examples stored only in the disposable demo database. */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoExperienceInitializer {
    private final UserRepository users;
    private final PetRepository pets;
    private final WeightRecordRepository weights;
    private final DailyLogRepository logs;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final RentalRecordRepository bills;

    @EventListener(ApplicationReadyEvent.class)
    @org.springframework.core.annotation.Order(10)
    @Transactional
    public void seed() {
        User owner = users.findByUsername("demo").orElseThrow();
        User merchant = users.findByUsername("demo_shop").orElseThrow();
        var samplePets = pets.findByOwnerId(owner.getId());
        if (weights.count() == 0) for (Pet pet : samplePets) {
            for (int i = 0; i < 3; i++) {
                WeightRecord record = new WeightRecord();
                record.setPet(pet); record.setWeight(Math.round((pet.getWeight().doubleValue() - .2 + i * .1) * 100) / 100.0);
                record.setRecordDate(LocalDate.now().minusDays(14 - i * 7));
                record.setNotes("演示称重记录，可新增或删除体验趋势变化"); weights.save(record);
            }
        }
        if (logs.count() == 0) for (Pet pet : samplePets) {
            DailyLog log = new DailyLog(); log.setPetId(pet.getId()); log.setUserId(owner.getId());
            log.setLogDate(LocalDate.now()); log.setLogType("daily");
            log.setTitle(pet.getName() + "的一天（样例）");
            log.setContent("正常进食、饮水，完成日常玩耍。点击编辑可记录自己的观察。"); logs.save(log);
        }
        if (orders.count() == 0) {
            Product product = products.findAll().stream().filter(p -> p.getMerchant().getId().equals(merchant.getId())).findFirst().orElseThrow();
            Order order = Order.builder().user(owner).status("pending").totalAmount(product.getPrice())
                .recipientName("样例收件人").recipientPhone("13800000000")
                .addressProvince("演示省").addressCity("演示市").addressDetail("虚构地址，仅供演示")
                .note("演示订单，可取消或由商户更新状态；未发生真实支付").build();
            OrderItem item = new OrderItem(); item.setOrder(order); item.setProduct(product);
            item.setProductName(product.getName()); item.setProductPrice(product.getPrice());
            item.setQuantity(1); item.setSubtotal(product.getPrice()); order.getItems().add(item);
            product.setStock(product.getStock() - 1); products.save(product); orders.save(order);
        }
        if (bills.count() == 0) bills.save(RentalRecord.builder().merchant(merchant).amount(300.0)
            .periodStart(LocalDate.now().withDayOfMonth(1)).periodEnd(LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1))
            .transactionId("DEMO-RENT-001").paymentMethod("bank").note("虚构租金账单，仅展示查询；未实际扣款").build());
    }
}
