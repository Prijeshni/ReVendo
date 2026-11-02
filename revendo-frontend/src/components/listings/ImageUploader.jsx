import React, { useRef, useState } from 'react';
import { isValidImageFile, isValidImageSize } from '../../utils/validators';

const MAX_IMAGES = 5;
const MAX_SIZE_MB = 5;

/**
 * Drag-and-drop / click-to-upload image input for listing forms.
 *
 * Props:
 *   images    {File[]}      current list of selected files
 *   onChange  {function}    called with new File[] when selection changes
 *
 * Validates: JPG/JPEG/PNG only, ≤ 5 MB each, max 5 total.
 * Requirements: 5.5, 5.7–5.9, 5.11, 15.7
 */
function ImageUploader({ images = [], onChange }) {
  const inputRef = useRef(null);
  const [dragOver, setDragOver] = useState(false);
  const [errors, setErrors] = useState([]);

  const addFiles = (fileList) => {
    const newErrors = [];
    const toAdd = [];

    Array.from(fileList).forEach((file) => {
      if (images.length + toAdd.length >= MAX_IMAGES) {
        newErrors.push(`Maximum of ${MAX_IMAGES} images allowed.`);
        return;
      }
      if (!isValidImageFile(file)) {
        newErrors.push(`"${file.name}" — only JPG, JPEG and PNG files are accepted.`);
        return;
      }
      if (!isValidImageSize(file)) {
        newErrors.push(`"${file.name}" — each image must not exceed ${MAX_SIZE_MB} MB.`);
        return;
      }
      toAdd.push(file);
    });

    setErrors(newErrors);
    if (toAdd.length > 0) {
      onChange([...images, ...toAdd]);
    }
  };

  const remove = (index) => {
    const updated = images.filter((_, i) => i !== index);
    onChange(updated);
    setErrors([]);
  };

  const onDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    addFiles(e.dataTransfer.files);
  };

  const onInputChange = (e) => {
    addFiles(e.target.files);
    e.target.value = '';           // allow re-selecting the same file
  };

  const canAddMore = images.length < MAX_IMAGES;

  return (
    <div>
      {/* Drop zone */}
      {canAddMore && (
        <div
          className={`image-uploader-drop${dragOver ? ' drag-over' : ''}`}
          onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={onDrop}
          onClick={() => inputRef.current?.click()}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === 'Enter' && inputRef.current?.click()}
          aria-label="Upload images"
        >
          <div style={{ fontSize: '2rem', marginBottom: 8 }}>📸</div>
          <p className="mb-1 fw-500" style={{ fontWeight: 500 }}>
            Drag &amp; drop images here, or click to select
          </p>
          <p className="text-muted mb-0" style={{ fontSize: '0.8rem' }}>
            JPG, JPEG, PNG · Max {MAX_SIZE_MB} MB each · Up to {MAX_IMAGES} images
            ({MAX_IMAGES - images.length} remaining)
          </p>
          <input
            ref={inputRef}
            type="file"
            accept=".jpg,.jpeg,.png"
            multiple
            style={{ display: 'none' }}
            onChange={onInputChange}
          />
        </div>
      )}

      {/* Preview grid */}
      {images.length > 0 && (
        <div className="d-flex flex-wrap gap-2 mt-3">
          {images.map((file, idx) => (
            <div key={idx} className="image-preview-wrapper">
              <img
                src={URL.createObjectURL(file)}
                alt={`Preview ${idx + 1}`}
                className="image-preview-thumb"
              />
              <button
                type="button"
                className="image-preview-remove"
                onClick={() => remove(idx)}
                aria-label={`Remove image ${idx + 1}`}
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Max reached message */}
      {!canAddMore && (
        <p className="text-muted mt-2 mb-0" style={{ fontSize: '0.82rem' }}>
          ✓ Maximum of {MAX_IMAGES} images reached. Remove one to add another.
        </p>
      )}

      {/* Validation errors */}
      {errors.length > 0 && (
        <div className="mt-2">
          {errors.map((err, i) => (
            <span key={i} className="field-error d-block">{err}</span>
          ))}
        </div>
      )}
    </div>
  );
}

export default ImageUploader;
