- Pipeline khởi đầu:

1. Paste .claude và `CLAUDE.md` template vào root folder
2. Tạo các subproject với coding standard chuẩn và đưa vào root directory có chứa sẵn `.claude` và `CLAUDE.md`
3. Paste các project requirement doc vào trong `.claude\requirements` ở ngoài cùng
4. Prompt root agent để chạy `apply-requirements`pipeline để apply requirement vào các `.claude` và `CLAUDE`trong các specific subproject đã tạo 
(CÓ THỂ SKIP BƯỚC NÀY, CHỜ CHO TỚI KHI CÓ WORKTREE RỒI CHẠY CŨNG ĐƯỢC)
5. xem xét và chỉnh sửa các file doc liên quan đến requirement đã map ra trong bước 4
(CÓ THỂ SKIP BƯỚC NÀY, CHỜ CHO TỚI KHI CÓ WORKTREE RỒI CHẠY CŨNG ĐƯỢC)
6. Prompt root agent để chạy `init-team` để creates real agent files, spawns team
7. Prompt root agent để chạy`init-worktrees`với các worktree mong muốn và agent team assignment để root agent tạo git worktree kèm với `.claude` và `CLAUDE.md` 
    
    (BƯỚC NÀY CÓ KÈM VỚI STEP RE-SPAWN TEAM NÊN YÊN TÂM VỤ AGENT KỊP CẬP NHẬT ROLE MỚI NHẤT)
    
8. Prompt root agent để chạy `apply-requirements`pipeline để apply requirement vào các `.claude` và `CLAUDE.md` trong các specific subproject (cả trong `.worktree` nếu có từ bước 5, là nếu có chạy bước 5 trước đó) đã tạo

- Pipeline khi có refresh git worktree:

1. Prompt root agent để chạy`init-worktrees`với các worktree mong muốn và agent team assignment để root agent tạo git worktree kèm với  `.claude` và `CLAUDE.md` 
    
    (BƯỚC NÀY CÓ KÈM VỚI STEP RE-SPAWN TEAM NÊN YÊN TÂM VỤ AGENT KỊP CẬP NHẬT ROLE MỚI NHẤT)
    
2. Prompt root agent để chạy `apply-requirements`pipeline để apply requirement vào các `.claude` và `CLAUDE.md` trong các specific subproject và các worktree subproject vừa tạo từ worktree mới ở bước 1

- Pipeline làm việc với worktrees, init + merge + refresh:

1. `init-worktrees`creates `.worktrees/taskflow-fe/` on `feat/frontend`
2. Teammate A works + commits there
3. ← worktree just sits there, `current standing branch` code is untouched (example: .dev)
4. You say "merge and clean up"
5. Root agent merges `feat/frontend` → `Current standing branch` (example: .dev)
6. Original `taskflow-fe/` on `current standing branch` now has the new code
7. Root agent removes the `worktree`
8. `.worktrees/taskflow-fe/` is gone