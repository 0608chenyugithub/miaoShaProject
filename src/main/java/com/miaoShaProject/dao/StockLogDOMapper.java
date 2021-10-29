package com.miaoShaProject.dao;

import com.miaoShaProject.dataObject.StockLogDO;
import com.miaoShaProject.dataObject.StockLogDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface StockLogDOMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    long countByExample(StockLogDOExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    int deleteByExample(StockLogDOExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    int deleteByPrimaryKey(String stockLogId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    int insert(StockLogDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    int insertSelective(StockLogDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    List<StockLogDO> selectByExample(StockLogDOExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    StockLogDO selectByPrimaryKey(String stockLogId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    int updateByExampleSelective(@Param("record") StockLogDO record, @Param("example") StockLogDOExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    int updateByExample(@Param("record") StockLogDO record, @Param("example") StockLogDOExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    int updateByPrimaryKeySelective(StockLogDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Sun Sep 26 16:57:46 CST 2021
     */
    int updateByPrimaryKey(StockLogDO record);
}