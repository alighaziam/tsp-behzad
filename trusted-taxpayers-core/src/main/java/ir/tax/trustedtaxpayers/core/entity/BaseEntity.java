package ir.tax.trustedtaxpayers.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ir.tax.trustedtaxpayers.core.util.UuidStringConverter;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * BaseEntity حاوی فیلدهای مشترک بین تمام موجودیت‌ها
 * 
 * <p>ویژگی‌ها:</p>
 * <ul>
 *   <li>UUID در دیتابیس ذخیره می‌شود</li>
 *   <li>در API به صورت String (encoded) نمایش داده می‌شود</li>
 *   <li>پشتیبانی از Auditing (created/updated dates and users)</li>
 *   <li>پشتیبانی از soft delete با isActive flag</li>
 * </ul>
 * 
 * @param <ID> نوع شناسه (معمولاً UUID)
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public abstract class BaseEntity implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
    @JdbcTypeCode(Types.BINARY)
	@Convert(converter = RawUuidConverter.class)
	@Column(name = "ID", nullable = false, updatable = false, columnDefinition = "RAW(16)")
	private UUID id;
	
	/**
	 * ID به صورت String برای نمایش در API (transient - در دیتابیس ذخیره نمی‌شود)
	 * این فیلد به صورت lazy محاسبه می‌شود
	 */
	@Transient
	private transient String idString;

	@Column(name = "IS_ACTIVE", nullable = false)
	@Builder.Default
	private Boolean isActive = true;

	@CreatedDate
	@Column(name = "CREATED_ON", nullable = false, updatable = false)
	private OffsetDateTime createdOn;

	@LastModifiedDate
	@Column(name = "UPDATED_ON")
	private OffsetDateTime updatedOn;

	@CreatedBy
	@Column(name = "CREATED_BY", length = 100, updatable = false)
	private String createdBy;

	@LastModifiedBy
	@Column(name = "UPDATED_BY", length = 100)
	private String updatedBy;

	/**
	 * قبل از persist شدن، اگر ID وجود نداشته باشد، یک UUID جدید تولید می‌کند
	 */
	@PrePersist
	protected void ensureId() {
		if (this.id == null) {
			this.id = UUID.randomUUID();
		}
	}
	
	/**
	 * دریافت ID به صورت String (encoded)
	 * این متد به صورت lazy عمل می‌کند و نتیجه را cache می‌کند
	 * 
	 * @return String encoded ID یا null اگر ID وجود نداشته باشد
	 */
	public String getIdString() {
		if (this.id == null) {
			return null;
		}
		if (this.idString == null) {
			this.idString = UuidStringConverter.encode(this.id);
		}
		return this.idString;
	}
	
	/**
	 * تنظیم ID از String (decoded)
	 * 
	 * @param idString String encoded ID
	 * @throws IllegalArgumentException اگر فرمت String نامعتبر باشد
	 */
	public void setIdString(String idString) {
		this.idString = idString;
		if (idString != null && !idString.isBlank()) {
			try {
				this.id = UuidStringConverter.decode(idString);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid ID string format: " + idString, e);
			}
		}
	}
	
	/**
	 * بررسی اینکه موجودیت جدید است یا نه (هنوز persist نشده)
	 * 
	 * @return true اگر موجودیت جدید باشد
	 */
	public boolean isNew() {
		return this.id == null;
	}
	
	/**
	 * غیرفعال کردن موجودیت (soft delete)
	 */
	public void deactivate() {
		this.isActive = false;
	}
	
	/**
	 * فعال کردن موجودیت
	 */
	public void activate() {
		this.isActive = true;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BaseEntity that = (BaseEntity) o;
		return Objects.equals(id, that.id);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
				"id=" + id +
				", isActive=" + isActive +
				", createdOn=" + createdOn +
				'}';
	}
}
