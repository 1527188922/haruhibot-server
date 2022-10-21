package com.haruhi.botServer.service.verbalTricks;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.VerbalTricks;

public interface VerbalTricksService extends IService<VerbalTricks> {

    void loadVerbalTricks();
}
