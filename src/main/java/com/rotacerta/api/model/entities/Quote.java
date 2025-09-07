package com.rotacerta.api.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "quotes")
@Data
@NoArgsConstructor
public class Quote {

	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="user_id", nullable=false)
	private User user;

	@Column(name="origin_zip", nullable=false)
	private String originZip;

	@Column(name="dest_zip", nullable=false)
	private String destZip;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition="jsonb")
	private String payload;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition="jsonb")
	private String result;

	@CreationTimestamp
	private OffsetDateTime createdAt;
}