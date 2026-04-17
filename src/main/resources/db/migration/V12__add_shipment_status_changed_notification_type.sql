ALTER TABLE in_app_notifications
    DROP CONSTRAINT chk_in_app_notification_type;

ALTER TABLE in_app_notifications
    ADD CONSTRAINT chk_in_app_notification_type CHECK (
        type IN ('AUCTION_WON', 'PAYMENT_DECLARED', 'PAYMENT_CONFIRMED', 'PAYMENT_DISPUTED', 'SHIPMENT_STATUS_CHANGED')
    );
