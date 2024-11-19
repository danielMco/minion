package com.example.minion.client;

import com.example.minion.model.Minion;
import com.example.minion.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "masterClient", url = "${master.url}")
public interface MasterClient {
    @PostMapping("/master/registerMinion")
    void registerMinion(@RequestBody Minion minion);

    @PostMapping("/master/receiveResult")
    void sendResult(@RequestBody Result result);
}
