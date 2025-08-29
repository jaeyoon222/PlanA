package com.study.StudyCafe.controller;

import com.study.StudyCafe.entity.Reservation;
import com.study.StudyCafe.repository.ReservationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class CheckInController {

    private final ReservationRepository reservationRepository;

    public CheckInController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/checkin/{token}")
    public ModelAndView checkIn(@PathVariable String token) {
        ModelAndView mv = new ModelAndView("checkin-result");

        Optional<Reservation> optional = reservationRepository.findByQrToken(token);

        if (optional.isEmpty()) {
            mv.addObject("message", "❌ 유효하지 않은 QR입니다.");
            return mv;
        }

        Reservation res = optional.get();

        if (res.isUsed()) {
            mv.addObject("message", "⚠️ 이미 사용된 QR입니다.");
            return mv;
        }

        res.setUsed(true);
        reservationRepository.save(res);

        mv.addObject("message", "🎉 입장이 완료되었습니다!");
        return mv;
    }
}
