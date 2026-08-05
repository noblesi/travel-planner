package com.noblesi.travelplanner.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.mapper.PlanEditOperationMapper;

@Component
class ScheduleOperationLedger {

	private final PlanEditOperationMapper planEditOperationMapper;

	ScheduleOperationLedger(PlanEditOperationMapper planEditOperationMapper) {
		this.planEditOperationMapper = planEditOperationMapper;
	}

	PlanEditOperation findReplay(
			String operationId,
			long planId,
			long memberId,
			ScheduleOperationType operationType,
			int baseVersion,
			String requestHash
	) {
		PlanEditOperation existing = planEditOperationMapper.findByOperationId(operationId);
		if (existing == null) {
			return null;
		}
		if (existing.planId() != planId
				|| existing.memberId() != memberId
				|| existing.operationType() != operationType
				|| existing.baseVersion() != baseVersion
				|| !existing.requestHash().equals(requestHash)) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					"DUPLICATE_OPERATION",
					"같은 작업 ID가 다른 요청에 이미 사용되었습니다."
			);
		}
		return existing;
	}

	void record(PlanEditOperation operation) {
		int affectedRows = planEditOperationMapper.insertOperation(operation);
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
	}

	String normalizeOperationId(String operationId) {
		return operationId.toLowerCase(Locale.ROOT);
	}

	String requestHash(Object... values) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (Object value : values) {
				String canonicalValue;
				if (value instanceof List<?> list) {
					canonicalValue = list.stream().map(String::valueOf)
							.collect(java.util.stream.Collectors.joining(","));
				} else if (value instanceof BigDecimal decimal) {
					canonicalValue = decimal.stripTrailingZeros().toPlainString();
				} else {
					canonicalValue = Objects.toString(value, "<null>");
				}
				byte[] bytes = canonicalValue.getBytes(StandardCharsets.UTF_8);
				digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
				digest.update((byte) ':');
				digest.update(bytes);
				digest.update((byte) ';');
			}
			return java.util.HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
