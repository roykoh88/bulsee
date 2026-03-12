package com.bulsee.vo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "WILDFIRE_FAQ")
public class FAQ {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "faq_seq")
    @SequenceGenerator(
            name = "faq_seq",
            sequenceName = "FAQ_SEQ",
            allocationSize = 1
    )
    @Column(name = "FAQ_ID")
    private Long faqId;

    @Column(name = "CATEGORY", nullable = false, length = 50)
    private String category;  // 일반, 예방, 대응, 통계

    @Column(name = "QUESTION", nullable = false, length = 500)
    private String question;

    @Column(name = "ANSWER", nullable = false, length = 2000)
    private String answer;

    @Column(name = "KEYWORDS", length = 500)
    private String keywords;  // 검색용 (쉼표 구분)

    @Column(name = "VIEW_COUNT")
    private Integer viewCount = 0;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
