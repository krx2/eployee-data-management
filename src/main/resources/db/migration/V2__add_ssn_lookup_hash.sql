ALTER TABLE employee ADD COLUMN ssn_lookup_hash VARCHAR(64) NOT NULL;

CREATE UNIQUE INDEX uq_employee_ssn_lookup_hash ON employee (ssn_lookup_hash);
