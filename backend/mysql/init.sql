USE chameidor;

CREATE TABLE task (
    id INT AUTO_INCREMENT PRIMARY KEY,
    host VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    data JSON NULL,
    cron VARCHAR(20) NULL,
    status VARCHAR(30) NOT NULL,
    executed_at DATETIME NULL,
    next_execution_at DATETIME NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,

    INDEX idx_status_next_execution (status, next_execution_at)
);

CREATE TABLE task_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    host VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    data JSON NULL,
    cron VARCHAR(20) NULL,
    status VARCHAR(30) NOT NULL,
    executed_at DATETIME NULL,
    next_execution_at DATETIME NULL,
    created_at DATETIME NOT NULL,

    FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE TABLE task_execution (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    executed_at DATETIME NOT NULL,
    duration_ms BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    result JSON NULL,
    created_at DATETIME NOT NULL,

    FOREIGN KEY (task_id) REFERENCES task(id)
);
