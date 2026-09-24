package com.petmgmt.service;

import com.petmgmt.model.Hospital;
import com.petmgmt.repository.HospitalRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    public List<Map<String, Object>> getAllHospitals() {
        seedHospitals();
        return hospitalRepository.findAll().stream().map(Hospital::toDict).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getNearbyHospitals(double lat, double lng, double radius) {
        seedHospitals();
        return hospitalRepository.findNearby(lat, lng, radius).stream().map(Hospital::toDict).collect(Collectors.toList());
    }

    public Hospital getHospital(Long id) {
        return hospitalRepository.findById(id).orElse(null);
    }

    private void seedHospitals() {
        if (hospitalRepository.count() > 0) return;

        List<Hospital> hospitals = List.of(
            createHospital("爱心宠物医院", "北京市朝阳区建国路88号", "010-88886666", 39.9087, 116.4716,
                "疫苗接种,体检,手术,寄养,美容", "周一至周日 9:00-21:00"),
            createHospital("瑞鹏宠物医院", "北京市海淀区中关村大街15号", "010-66668888", 39.9836, 116.3168,
                "全科诊疗,手术,住院,影像诊断", "周一至周日 8:30-22:00"),
            createHospital("美联众合动物医院", "上海市浦东新区陆家嘴环路1000号", "021-68889999", 31.2357, 121.5016,
                "专科诊疗,手术,中医康复,体检", "周一至周日 9:00-20:30"),
            createHospital("芭比堂动物医院", "广州市天河区天河路385号", "020-88887777", 23.1291, 113.3314,
                "眼科专科,全科,手术,体检", "周一至周日 9:00-18:00"),
            createHospital("派特宠物医院", "深圳市南山区科技园路1号", "0755-66669999", 22.5431, 113.9531,
                "全科诊疗,手术,住院,美容", "周一至周日 10:00-19:00")
        );
        hospitalRepository.saveAll(hospitals);
    }

    private Hospital createHospital(String name, String address, String phone,
                                     double lat, double lng, String services, String hours) {
        Hospital h = new Hospital();
        h.setName(name); h.setAddress(address); h.setPhone(phone);
        h.setLatitude(lat); h.setLongitude(lng);
        h.setServices(services); h.setHours(hours);
        return h;
    }
}
