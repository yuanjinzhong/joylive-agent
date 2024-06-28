/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.demo.springcloud.v3.order.servcice.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jd.live.agent.demo.springcloud.v3.order.entity.User;
import com.jd.live.agent.demo.springcloud.v3.order.mapper.UserMapper;
import com.jd.live.agent.demo.springcloud.v3.order.servcice.UserService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    @Cacheable(value = "user", key = "#id")
    public User getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Cacheable(value = "userExists", key = "#id")
    public boolean userExists(Long id) {
        return getById(id) != null;
    }
}