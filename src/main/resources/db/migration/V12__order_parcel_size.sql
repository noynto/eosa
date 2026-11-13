-- Dimensions du colis saisies à la fabrication, conservées avec son poids (parcel_weight_grams).
ALTER TABLE orders
    ADD COLUMN parcel_length_cm INT,
    ADD COLUMN parcel_width_cm INT,
    ADD COLUMN parcel_depth_cm INT;
