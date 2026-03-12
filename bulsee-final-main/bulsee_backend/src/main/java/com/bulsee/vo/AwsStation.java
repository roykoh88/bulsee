package com.bulsee.vo;

import jakarta.persistence.*;
        import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "AWS_STATION") // 오라클 테이블 이름
public class AwsStation {

    @Id // PK (Primary Key)
    @Column(name = "STN_ID")
    private Long stn; // 관측소 ID (NUMBER 10)

    @Column(name = "LAT")
    private Double lat; // 위도

    @Column(name = "LON")
    private Double lon; // 경도

    @Column(name = "NAME")
    private String name; // 관측소 이름

    @Column(name = "MAPPING")
    private String mapping; // '지역내' 또는 '외부최근접'

   // @Column(name = "CREATED_AT")
   // private LocalDateTime createdAt; // 생성일 (DATE)
}