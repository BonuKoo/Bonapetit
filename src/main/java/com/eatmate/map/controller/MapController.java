package com.eatmate.map.controller;

import com.eatmate.map.vo.MapVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class MapController {

    @GetMapping("/map")
    public String mapPage() {
        return "map/map";
    }

    @PostMapping("/map")
    @ResponseBody
    public ResponseEntity<MapVo> getMap(@RequestBody MapVo mapVo) {
        return ResponseEntity.ok(mapVo);
    }

}
