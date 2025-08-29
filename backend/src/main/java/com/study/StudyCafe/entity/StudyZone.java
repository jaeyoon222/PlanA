package com.study.StudyCafe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "study_zone")
@Getter
@Setter
public class StudyZone {

    @Id
    @GeneratedValue
    private Long id;

    private String zoneName;
    private String description;

    @OneToMany(mappedBy = "zone")
    @JsonIgnore
    private List<Seat> seats;

    private double latitude;
    private double longitude;
}
