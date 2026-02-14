-- V4: Create KYC documents table

CREATE TABLE kyc_documents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    document_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMP,
    reviewed_by UUID REFERENCES users (id)
);

CREATE INDEX idx_kyc_documents_user ON kyc_documents (user_id);

CREATE INDEX idx_kyc_documents_status ON kyc_documents (status);