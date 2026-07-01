CREATE TABLE buyer_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL,
    reporter_name VARCHAR(150) NOT NULL,
    reported_user_id UUID NOT NULL,
    reported_user_name VARCHAR(150) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE buyer_report_images (
    report_id UUID NOT NULL REFERENCES buyer_reports(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL
);

CREATE INDEX idx_buyer_reports_status ON buyer_reports(status);
CREATE INDEX idx_buyer_reports_reported_user_id ON buyer_reports(reported_user_id);
CREATE INDEX idx_buyer_report_images_report_id ON buyer_report_images(report_id);
