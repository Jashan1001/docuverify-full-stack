--
-- PostgreSQL database dump
--

\restrict cJCgpztaiLAs1SHHM1b5egHg5bgDAYvtFf9wZbcVwGSYJDgmRzD7bUGdbDiQaIN

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: documents; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.documents (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(255),
    file_hash character varying(255) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_size bigint NOT NULL,
    file_type character varying(255) NOT NULL,
    file_url character varying(255) NOT NULL,
    rejection_reason character varying(255),
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    verification_token character varying(255) NOT NULL,
    institution_id uuid NOT NULL,
    user_id uuid NOT NULL,
    verified_by uuid,
    CONSTRAINT documents_status_check CHECK (((status)::text = ANY ((ARRAY['UPLOADED'::character varying, 'UNDER_REVIEW'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.documents OWNER TO postgres;

--
-- Name: institutions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.institutions (
    id uuid NOT NULL,
    active boolean NOT NULL,
    contact_email character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    domain character varying(255) NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.institutions OWNER TO postgres;

--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.refresh_tokens (
    id uuid NOT NULL,
    expiry_date timestamp(6) without time zone NOT NULL,
    revoked boolean NOT NULL,
    token character varying(512) NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.refresh_tokens OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    email character varying(255) NOT NULL,
    enabled boolean NOT NULL,
    full_name character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    institution_id uuid,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['ROLE_USER'::character varying, 'ROLE_VERIFIER'::character varying, 'ROLE_ADMIN'::character varying, 'ROLE_INSTITUTION_ADMIN'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: verification_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.verification_logs (
    id uuid NOT NULL,
    action character varying(255) NOT NULL,
    ip_address character varying(255),
    performed_by character varying(255),
    remarks character varying(255),
    "timestamp" timestamp(6) without time zone NOT NULL,
    document_id uuid NOT NULL,
    CONSTRAINT verification_logs_action_check CHECK (((action)::text = ANY ((ARRAY['UPLOADED'::character varying, 'SUBMITTED_FOR_REVIEW'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'VIEWED'::character varying, 'PUBLIC_VERIFIED'::character varying, 'DELETED'::character varying])::text[])))
);


ALTER TABLE public.verification_logs OWNER TO postgres;

--
-- Name: documents documents_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);


--
-- Name: institutions institutions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: documents uk_4a2bhylex3qt9r9cb4ag75yx9; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT uk_4a2bhylex3qt9r9cb4ag75yx9 UNIQUE (verification_token);


--
-- Name: institutions uk_5v1fmk528u7n0jx83osn75j6k; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT uk_5v1fmk528u7n0jx83osn75j6k UNIQUE (domain);


--
-- Name: users uk_6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: refresh_tokens uk_ghpmfn23vmxfu3spu3lfg4r2d; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT uk_ghpmfn23vmxfu3spu3lfg4r2d UNIQUE (token);


--
-- Name: documents uk_ilhsw8on9lq3syydv1qdscexx; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT uk_ilhsw8on9lq3syydv1qdscexx UNIQUE (file_hash);


--
-- Name: institutions uk_l3uewvcgggjpwyvk8gu8rs2b9; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT uk_l3uewvcgggjpwyvk8gu8rs2b9 UNIQUE (name);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: verification_logs verification_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verification_logs
    ADD CONSTRAINT verification_logs_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens fk1lih5y2npsf8u5o3vhdb9y0os; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: users fk2qqjpih9isqcs22710v8lef9w; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk2qqjpih9isqcs22710v8lef9w FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: documents fk45huj8qshqfs09gxjuovmnvmp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk45huj8qshqfs09gxjuovmnvmp FOREIGN KEY (institution_id) REFERENCES public.institutions(id);


--
-- Name: documents fkkxttj4tp5le2uth212lu49vny; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fkkxttj4tp5le2uth212lu49vny FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: verification_logs fkoa3pa3se0tyqb5heha9c1jc6s; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.verification_logs
    ADD CONSTRAINT fkoa3pa3se0tyqb5heha9c1jc6s FOREIGN KEY (document_id) REFERENCES public.documents(id);


--
-- Name: documents fkq17nnmpcqeup72ldkglft9fuo; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fkq17nnmpcqeup72ldkglft9fuo FOREIGN KEY (verified_by) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict cJCgpztaiLAs1SHHM1b5egHg5bgDAYvtFf9wZbcVwGSYJDgmRzD7bUGdbDiQaIN

