package com.study.StudyCafe.controller;

import com.study.StudyCafe.dto.NLP.BranchDto;
import com.study.StudyCafe.entity.StudyZone;
import com.study.StudyCafe.repository.StudyZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final StudyZoneRepository studyZoneRepository;

    @GetMapping
    public List<BranchDto> getBranches() {
        return studyZoneRepository.findAll().stream()
                .map(z -> new BranchDto(
                        z.getZoneName() != null ? z.getZoneName() : "지점#" + z.getId(),
                        z.getId()
                ))
                .collect(Collectors.toList());
    }
}
