INSERT INTO restaurant_tables(table_number, capacity) VALUES (1, 2),
                                                  (2,2),
                                                  (3,4);

INSERT INTO bookings(start_time, end_time, table_number, persons, guest_id, status)
VALUES ('2025-07-13 13:00:00', '2025-07-13 14:00:00', 1, 1, 1, 'CONFIRMED'),
       ('2025-07-14 12:00:00', '2025-07-14 14:00:00', 2, 2, 2, 'CONFIRMED'),
       ('2025-07-18 23:00:00', '2025-07-19 01:00:00', 3, 2, 2, 'CONFIRMED');