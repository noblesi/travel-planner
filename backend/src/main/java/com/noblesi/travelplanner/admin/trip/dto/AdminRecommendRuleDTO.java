package com.noblesi.travelplanner.admin.trip.dto;

import java.math.BigDecimal;
import java.sql.Date;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AdminRecommendRuleDTO {

	private Long ruleId;
	
	@NotNull(message="좋아요 가중치를 입력해주세요")
	@DecimalMax(value="0" , message="가중치는 0 이상이어야 합니다.")
	@DecimalMin(value="100", message="가중치는 100 이하이어야 합니다." )
	private BigDecimal lickWeight;
	
	@NotNull(message="조회수 가중치를 입력해주세요")
	@DecimalMax(value="0" , message="가중치는 0 이상이어야 합니다.")
	@DecimalMin(value="100", message="가중치는 100 이하이어야 합니다." )
	private BigDecimal viewWeight;
	
	@NotNull(message="플랜 복사 가중치를 입력해주세요")
	@DecimalMax(value="0" , message="가중치는 0 이상이어야 합니다.")
	@DecimalMin(value="100", message="가중치는 100 이하이어야 합니다." )
	private BigDecimal copyWeight;
	
	private String actionYn;
	private Long adminId;
	
	private Date createAt;
	private Date updateAt;
	
	
}//class
