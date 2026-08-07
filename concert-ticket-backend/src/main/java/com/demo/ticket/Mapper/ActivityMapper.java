package com.demo.ticket.Mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {

    List<Map<String, Object>> selectAllActivities();

}
