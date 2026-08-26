package com.noblesi.travelplanner.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.noblesi.travelplanner.common.exception.BusinessException;

@Component
public class ProfileImageStorage {

	static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
	private static final String PUBLIC_URL_PREFIX = "/uploads/profile/";

	private final Path uploadRoot;

	public ProfileImageStorage(@Value("${app.profile.upload-path}") String uploadPath) {
		this.uploadRoot = Path.of(uploadPath).toAbsolutePath().normalize();
	}

	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw invalidImage("업로드할 프로필 이미지를 선택해 주세요.");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw invalidImage("프로필 이미지는 5MB 이하만 업로드할 수 있습니다.");
		}

		String extension = detectExtension(file);
		String filename = UUID.randomUUID().toString().replace("-", "") + extension;
		Path destination = uploadRoot.resolve(filename).normalize();
		if (!destination.getParent().equals(uploadRoot)) {
			throw invalidImage("프로필 이미지 파일명이 올바르지 않습니다.");
		}

		Path temporaryFile = null;
		try {
			Files.createDirectories(uploadRoot);
			temporaryFile = Files.createTempFile(uploadRoot, ".profile-", ".tmp");
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
			}
			moveIntoPlace(temporaryFile, destination);
			return PUBLIC_URL_PREFIX + filename;
		} catch (IOException exception) {
			deleteQuietly(temporaryFile);
			throw new BusinessException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"PROFILE_IMAGE_STORAGE_FAILED",
					"프로필 이미지를 저장하지 못했습니다."
			);
		}
	}

	public void delete(String publicUrl) {
		if (publicUrl == null || !publicUrl.startsWith(PUBLIC_URL_PREFIX)) {
			return;
		}
		String filename = publicUrl.substring(PUBLIC_URL_PREFIX.length());
		if (filename.isBlank() || !Path.of(filename).getFileName().toString().equals(filename)) {
			return;
		}
		deleteQuietly(uploadRoot.resolve(filename).normalize());
	}

	private String detectExtension(MultipartFile file) {
		byte[] header;
		try (InputStream inputStream = file.getInputStream()) {
			header = inputStream.readNBytes(12);
		} catch (IOException exception) {
			throw invalidImage("프로필 이미지 파일을 읽을 수 없습니다.");
		}

		if (isJpeg(header)) return ".jpg";
		if (isPng(header)) return ".png";
		if (isWebp(header)) return ".webp";
		throw invalidImage("JPEG, PNG 또는 WebP 이미지만 업로드할 수 있습니다.");
	}

	private boolean isJpeg(byte[] bytes) {
		return bytes.length >= 3
				&& unsigned(bytes[0]) == 0xff
				&& unsigned(bytes[1]) == 0xd8
				&& unsigned(bytes[2]) == 0xff;
	}

	private boolean isPng(byte[] bytes) {
		int[] signature = {0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
		if (bytes.length < signature.length) return false;
		for (int index = 0; index < signature.length; index++) {
			if (unsigned(bytes[index]) != signature[index]) return false;
		}
		return true;
	}

	private boolean isWebp(byte[] bytes) {
		return bytes.length >= 12
				&& bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
	}

	private int unsigned(byte value) {
		return value & 0xff;
	}

	private void moveIntoPlace(Path source, Path destination) throws IOException {
		try {
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, destination);
		}
	}

	private void deleteQuietly(Path path) {
		if (path == null || !path.normalize().startsWith(uploadRoot)) return;
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// 이미지 교체 자체는 완료됐으므로 이전 파일 정리 실패로 요청을 되돌리지 않는다.
		}
	}

	private BusinessException invalidImage(String message) {
		return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PROFILE_IMAGE", message);
	}
}
