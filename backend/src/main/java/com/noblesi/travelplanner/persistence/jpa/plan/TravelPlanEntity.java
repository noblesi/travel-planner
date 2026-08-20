package com.noblesi.travelplanner.persistence.jpa.plan;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.noblesi.travelplanner.domain.plan.PlanPublishStatus;
import com.noblesi.travelplanner.domain.plan.PlanVisibility;
import com.noblesi.travelplanner.domain.plan.TravelPlan;
import com.noblesi.travelplanner.domain.plan.TravelPlanStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "TRAVEL_PLAN")
@DynamicUpdate
public class TravelPlanEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "travelPlanSequence")
	@SequenceGenerator(
			name = "travelPlanSequence",
			sequenceName = "SEQ_TRAVEL_PLAN",
			allocationSize = 1
	)
	@JdbcTypeCode(SqlTypes.NUMERIC)
	@Column(name = "PLAN_ID")
	private Long planId;

	@JdbcTypeCode(SqlTypes.NUMERIC)
	@Column(name = "OWNER_MEMBER_ID", nullable = false)
	private long ownerMemberId;

	@Column(name = "TITLE", nullable = false, length = 200)
	private String title;

	@Column(name = "REGION_CODE", nullable = false, length = 20)
	private String regionCode;

	@Column(name = "START_DATE", nullable = false)
	private LocalDate startDate;

	@Column(name = "END_DATE", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "VISIBILITY", nullable = false, length = 10)
	private PlanVisibility visibility;

	@Enumerated(EnumType.STRING)
	@Column(name = "PUBLISH_STATUS", nullable = false, length = 10)
	private PlanPublishStatus publishStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "PLAN_STATUS", nullable = false, length = 10)
	private TravelPlanStatus planStatus;

	@Version
	@JdbcTypeCode(SqlTypes.NUMERIC)
	@Column(name = "VERSION_NO", nullable = false)
	private int versionNo;

	@Column(name = "DELETED_AT")
	private OffsetDateTime deletedAt;

	@JdbcTypeCode(SqlTypes.NUMERIC)
	@Column(name = "DELETED_BY_MEMBER_ID")
	private Long deletedByMemberId;

	@Column(name = "THUMBNAIL_IMG", length = 1000)
	private String thumbnailImageUrl;

	@Column(name = "CREATED_AT", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "UPDATED_AT", nullable = false)
	private OffsetDateTime updatedAt;

	protected TravelPlanEntity() {
	}

	private TravelPlanEntity(
			long ownerMemberId,
			String title,
			String regionCode,
			LocalDate startDate,
			LocalDate endDate,
			PlanVisibility visibility
	) {
		this.ownerMemberId = ownerMemberId;
		this.title = title;
		this.regionCode = regionCode;
		this.startDate = startDate;
		this.endDate = endDate;
		this.visibility = visibility;
		this.publishStatus = PlanPublishStatus.DRAFT;
		this.planStatus = TravelPlanStatus.ACTIVE;
	}

	public static TravelPlanEntity create(
			long ownerMemberId,
			String title,
			String regionCode,
			LocalDate startDate,
			LocalDate endDate,
			PlanVisibility visibility
	) {
		return new TravelPlanEntity(
				ownerMemberId,
				title,
				regionCode,
				startDate,
				endDate,
				visibility
		);
	}

	@PrePersist
	void initializeTimestamps() {
		OffsetDateTime now = OffsetDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void updateTimestamp() {
		updatedAt = OffsetDateTime.now();
	}

	public void updateMetadata(String title, PlanVisibility visibility) {
		this.title = title;
		this.visibility = visibility;
	}

	public void updatePublication(
			PlanPublishStatus publishStatus,
			String fallbackThumbnailImageUrl
	) {
		this.publishStatus = publishStatus;
		if (thumbnailImageUrl == null) {
			thumbnailImageUrl = fallbackThumbnailImageUrl;
		}
	}

	public void softDelete(long memberId) {
		planStatus = TravelPlanStatus.DELETED;
		deletedAt = OffsetDateTime.now();
		deletedByMemberId = memberId;
	}

	public void restore() {
		planStatus = TravelPlanStatus.ACTIVE;
		deletedAt = null;
		deletedByMemberId = null;
	}

	public boolean hasSameMetadata(String title, PlanVisibility visibility) {
		return this.title.equals(title) && this.visibility == visibility;
	}

	public boolean hasPublishStatus(PlanPublishStatus publishStatus) {
		return this.publishStatus == publishStatus;
	}

	public boolean isActive() {
		return planStatus == TravelPlanStatus.ACTIVE;
	}

	public TravelPlan toDomain() {
		return new TravelPlan(
				planId,
				ownerMemberId,
				title,
				regionCode,
				startDate,
				endDate,
				visibility,
				publishStatus
		);
	}

	public long getPlanId() {
		return planId;
	}

	public int getVersionNo() {
		return versionNo;
	}

	public PlanPublishStatus getPublishStatus() {
		return publishStatus;
	}

	public TravelPlanStatus getPlanStatus() {
		return planStatus;
	}
}
