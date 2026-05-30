package com.example.carrental.config;

import com.example.carrental.common.Enums.CarStatus;
import com.example.carrental.common.Enums.CommentStatus;
import com.example.carrental.common.Enums.ContractStatus;
import com.example.carrental.common.Enums.MaintenanceType;
import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.common.Enums.PayStatus;
import com.example.carrental.common.Enums.PayType;
import com.example.carrental.common.Enums.StoreStatus;
import com.example.carrental.common.Enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarCategory;
import com.example.carrental.domain.Comment;
import com.example.carrental.domain.Contract;
import com.example.carrental.domain.MaintenanceRecord;
import com.example.carrental.domain.PaymentOrder;
import com.example.carrental.domain.RentalOrder;
import com.example.carrental.domain.Store;
import com.example.carrental.domain.User;
import com.example.carrental.repository.CarCategoryRepository;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.CommentRepository;
import com.example.carrental.repository.ContractRepository;
import com.example.carrental.repository.MaintenanceRecordRepository;
import com.example.carrental.repository.PaymentOrderRepository;
import com.example.carrental.repository.RentalOrderRepository;
import com.example.carrental.repository.StoreRepository;
import com.example.carrental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @ConditionalOnProperty(name = "app.data-init.enabled", havingValue = "true")
    CommandLineRunner initDemoData(
            UserRepository userRepository,
            StoreRepository storeRepository,
            CarCategoryRepository categoryRepository,
            CarRepository carRepository,
            RentalOrderRepository orderRepository,
            PaymentOrderRepository paymentRepository,
            ContractRepository contractRepository,
            CommentRepository commentRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("DataInitializer: data already exists, skipping sample seed.");
                return;
            }
            log.info("DataInitializer: seeding sample data (users table is empty)...");

            User admin = user("admin", "123456", "18800000001", UserRole.ADMIN, passwordEncoder);
            User staff = user("staff", "123456", "18800000002", UserRole.STORE_STAFF, passwordEncoder);
            User customer = user("zhangsan", "123456", "18800000003", UserRole.USER, passwordEncoder);
            customer.setRealName("张三");
            customer.setIdCard("310101199001011234");
            customer.setDriverLicenseNo("DL3101019001011234");
            userRepository.saveAll(List.of(admin, staff, customer));

            Store hongqiao = store("上海虹桥旗舰店", "上海", "闵行区申虹路 88 号", "021-80010001");
            Store pudong = store("上海浦东机场店", "上海", "浦东新区迎宾大道 1200 号", "021-80010002");
            Store chaoyang = store("北京朝阳商务店", "北京", "朝阳区建国路 66 号", "010-80010003");
            storeRepository.saveAll(List.of(hongqiao, pudong, chaoyang));

            CarCategory economy = category("经济型", "城市通勤、短租高性价比车型");
            CarCategory business = category("商务型", "商务接待与长途舒适出行");
            CarCategory suv = category("SUV", "家庭旅行与复杂路况");
            CarCategory ev = category("新能源", "低碳出行与市区通勤");
            categoryRepository.saveAll(List.of(economy, business, suv, ev));

            Car lavida = car("大众朗逸 2024", "大众", "朗逸", economy, "沪A10001", hongqiao, "189.00", "1000.00", 15200,
                    "省油耐用，适合城市短途和工作日通勤。", "https://images.unsplash.com/photo-1549924231-f129b911e442");
            Car camry = car("丰田凯美瑞 2.5G", "丰田", "凯美瑞", business, "沪A20002", hongqiao, "329.00", "2000.00", 23100,
                    "舒适后排和稳定动力，适合商务接待。", "https://images.unsplash.com/photo-1552519507-da3b142c6e3d");
            Car model3 = car("特斯拉 Model 3", "特斯拉", "Model 3", ev, "沪D30003", pudong, "459.00", "3000.00", 9800,
                    "智能电动车型，支持快速取还车。", "https://images.unsplash.com/photo-1560958089-b8a1929cea89");
            Car gl8 = car("别克 GL8 陆尊", "别克", "GL8", business, "沪B40004", pudong, "599.00", "3500.00", 40200,
                    "七座商务 MPV，适合团队和家庭出行。", "https://images.unsplash.com/photo-1542362567-b07e54358753");
            Car tank300 = car("坦克 300 城市版", "坦克", "300", suv, "京A50005", chaoyang, "499.00", "3000.00", 17600,
                    "通过性好，适合周末自驾。", "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf");
            lavida.setStatus(CarStatus.RESERVED);
            camry.setStatus(CarStatus.RENTING);
            gl8.setStatus(CarStatus.MAINTAINING);
            carRepository.saveAll(List.of(lavida, camry, model3, gl8, tank300));

            LocalDateTime now = LocalDateTime.now().withNano(0);
            RentalOrder pendingPickupOrder = order(
                    "RO-DEMO-PICKUP",
                    customer,
                    lavida,
                    hongqiao,
                    hongqiao,
                    now.plusDays(1).withHour(10).withMinute(0),
                    3,
                    OrderStatus.PENDING_PICKUP
            );
            RentalOrder rentingOrder = order(
                    "RO-DEMO-RENTING",
                    customer,
                    camry,
                    hongqiao,
                    hongqiao,
                    now.minusDays(1).withHour(9).withMinute(30),
                    4,
                    OrderStatus.RENTING
            );
            RentalOrder completedOrder = order(
                    "RO-DEMO-COMPLETED",
                    customer,
                    model3,
                    pudong,
                    hongqiao,
                    now.minusDays(12).withHour(11).withMinute(0),
                    3,
                    OrderStatus.COMPLETED
            );
            orderRepository.saveAll(List.of(pendingPickupOrder, rentingOrder, completedOrder));

            paymentRepository.saveAll(List.of(
                    payment("PAY-DEMO-PICKUP", pendingPickupOrder, now.minusHours(2)),
                    payment("PAY-DEMO-RENTING", rentingOrder, now.minusDays(2)),
                    payment("PAY-DEMO-COMPLETED", completedOrder, now.minusDays(12))
            ));
            contractRepository.saveAll(List.of(
                    contract("CT-DEMO-PICKUP", pendingPickupOrder, ContractStatus.UNSIGNED),
                    contract("CT-DEMO-RENTING", rentingOrder, ContractStatus.UNSIGNED),
                    contract("CT-DEMO-COMPLETED", completedOrder, ContractStatus.SIGNED)
            ));
            commentRepository.save(comment(
                    customer,
                    model3,
                    completedOrder,
                    5,
                    "取还车都很顺畅，车辆干净，续航也够用。"
            ));
            maintenanceRecordRepository.save(maintenance(
                    gl8,
                    MaintenanceType.MAINTENANCE,
                    "3 万公里常规保养，已预约更换机油、空滤并完成内饰深度清洁。",
                    "680.00",
                    now.minusHours(5)
            ));
        };
    }

    private User user(String username, String password, String phone, UserRole role, PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setEmail(username + "@example.com");
        user.setRole(role);
        return user;
    }

    private Store store(String name, String city, String address, String phone) {
        Store store = new Store();
        store.setStoreName(name);
        store.setCity(city);
        store.setAddress(address);
        store.setPhone(phone);
        store.setBusinessHours("08:00-22:00");
        store.setStatus(StoreStatus.OPEN);
        return store;
    }

    private CarCategory category(String name, String description) {
        CarCategory category = new CarCategory();
        category.setCategoryName(name);
        category.setDescription(description);
        return category;
    }

    private Car car(
            String carName,
            String brand,
            String model,
            CarCategory category,
            String plateNumber,
            Store store,
            String pricePerDay,
            String deposit,
            int mileage,
            String description,
            String imageUrl
    ) {
        Car car = new Car();
        car.setCarName(carName);
        car.setBrand(brand);
        car.setModel(model);
        car.setCategory(category);
        car.setPlateNumber(plateNumber);
        car.setStore(store);
        car.setPricePerDay(new BigDecimal(pricePerDay));
        car.setDeposit(new BigDecimal(deposit));
        car.setStatus(CarStatus.AVAILABLE);
        car.setMileage(mileage);
        car.setDescription(description);
        car.replaceImages(List.of(imageUrl));
        return car;
    }

    private RentalOrder order(
            String orderNo,
            User user,
            Car car,
            Store pickupStore,
            Store returnStore,
            LocalDateTime startTime,
            int rentalDays,
            OrderStatus status
    ) {
        RentalOrder order = new RentalOrder();
        order.setOrderNo(orderNo);
        order.setUser(user);
        order.setCar(car);
        order.setPickupStore(pickupStore);
        order.setReturnStore(returnStore);
        order.setStartTime(startTime);
        order.setEndTime(startTime.plusDays(rentalDays));
        order.setRentalDays(rentalDays);
        order.setTotalAmount(car.getPricePerDay().multiply(BigDecimal.valueOf(rentalDays)));
        order.setDepositAmount(car.getDeposit());
        order.setStatus(status);
        return order;
    }

    private PaymentOrder payment(String paymentNo, RentalOrder order, LocalDateTime payTime) {
        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo(paymentNo);
        payment.setRentalOrder(order);
        payment.setUser(order.getUser());
        payment.setPayAmount(order.getTotalAmount().add(order.getDepositAmount()));
        payment.setPayType(PayType.MOCK);
        payment.setPayStatus(PayStatus.SUCCESS);
        payment.setTransactionNo("MOCK-" + paymentNo);
        payment.setPayTime(payTime);
        return payment;
    }

    private Contract contract(String contractNo, RentalOrder order, ContractStatus status) {
        Contract contract = new Contract();
        contract.setContractNo(contractNo);
        contract.setRentalOrder(order);
        contract.setUser(order.getUser());
        contract.setContractUrl("/files/contracts/" + contractNo + ".pdf");
        contract.setSignStatus(status);
        return contract;
    }

    private Comment comment(User user, Car car, RentalOrder order, int score, String content) {
        Comment comment = new Comment();
        comment.setUser(user);
        comment.setCar(car);
        comment.setRentalOrder(order);
        comment.setScore(score);
        comment.setContent(content);
        comment.setStatus(CommentStatus.APPROVED);
        return comment;
    }

    private MaintenanceRecord maintenance(
            Car car,
            MaintenanceType type,
            String description,
            String cost,
            LocalDateTime recordTime
    ) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setCar(car);
        record.setType(type);
        record.setDescription(description);
        record.setCost(new BigDecimal(cost));
        record.setRecordTime(recordTime);
        return record;
    }
}
